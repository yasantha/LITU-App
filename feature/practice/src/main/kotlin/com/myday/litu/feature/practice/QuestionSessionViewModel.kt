package com.myday.litu.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myday.litu.core.analytics.AnalyticsEvent
import com.myday.litu.core.analytics.AnalyticsLogger
import com.myday.litu.core.audio.ReadAloud
import com.myday.litu.core.audio.ReadAloudItem
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.domain.repository.AppInfo
import com.myday.litu.core.domain.repository.BackupScheduler
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.QuestionReport
import com.myday.litu.core.domain.repository.ReportReason
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.streak.StreakInfo
import com.myday.litu.core.domain.usecase.BuildPracticeSessionUseCase
import com.myday.litu.core.domain.usecase.FinishSessionUseCase
import com.myday.litu.core.domain.usecase.ObserveTodayUseCase
import com.myday.litu.core.domain.usecase.PracticeRequest
import com.myday.litu.core.domain.usecase.RecordAnswerUseCase
import com.myday.litu.core.domain.usecase.ToggleFlagUseCase
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionType
import com.myday.litu.core.model.StudyMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class MissedQuestion(val stem: String, val correctAnswer: String)

data class ChapterResult(val number: Int, val title: String, val score: ChapterScore)

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Locked : SessionUiState
    data object Empty : SessionUiState

    data class Answering(
        val index: Int,
        val total: Int,
        val question: Question,
        val options: List<AnswerOption>,
        val selected: Set<String>,
        val checked: Boolean,
        val wasCorrect: Boolean?,
        val flagged: Boolean,
        val audioPlaying: Boolean,
        val isSample: Boolean,
        val sectionTitle: String,
    ) : SessionUiState {
        val canCheck: Boolean get() = selected.size == question.type.requiredSelections
        val isLast: Boolean get() = index == total - 1
    }

    data class Summary(
        val answered: Int,
        val correct: Int,
        val missed: List<MissedQuestion>,
        val todayAnswered: Int,
        val goal: Int,
        val streak: StreakInfo,
        val isSample: Boolean,
        val chapterResults: List<ChapterResult>,
        val weakestChapter: String?,
    ) : SessionUiState
}

@HiltViewModel
class QuestionSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val buildSession: BuildPracticeSessionUseCase,
    private val recordAnswer: RecordAnswerUseCase,
    private val toggleFlag: ToggleFlagUseCase,
    private val finishSession: FinishSessionUseCase,
    private val observeToday: ObserveTodayUseCase,
    private val entitlements: EntitlementRepository,
    private val analytics: AnalyticsLogger,
    private val readAloud: ReadAloud,
    private val backupScheduler: BackupScheduler,
    private val appInfo: AppInfo,
    private val clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<QuestionSessionRoute>()
    private val request = route.toRequest()
    private val mode = when (request) {
        PracticeRequest.Sample -> StudyMode.SAMPLE
        PracticeRequest.Review -> StudyMode.REVIEW
        PracticeRequest.TimerQuiz -> StudyMode.TIMER
        else -> StudyMode.PRACTICE
    }

    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = mutableState.asStateFlow()

    private var ids: List<String> = emptyList()
    private var sessionId: String = ""
    private var startedAt = clock.millis()
    private var shownAt = clock.millis()
    private val results = mutableListOf<Pair<Question, Boolean>>()

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        // Free users get the sample and the review queue; everything else is behind the paywall.
        val free = request == PracticeRequest.Sample || request == PracticeRequest.Review
        if (!free && !entitlements.isPro.value) {
            mutableState.value = SessionUiState.Locked
            return
        }
        ids = buildSession(request)
        if (ids.isEmpty()) {
            mutableState.value = SessionUiState.Empty
            return
        }
        sessionId = progress.startSession(mode, clock.instant()).id
        startedAt = clock.millis()
        show(0)
    }

    private suspend fun show(index: Int) {
        val question = content.question(ids[index]) ?: return skip(index)
        // Options are shuffled at display time, except true/false.
        val options = if (question.type == QuestionType.TRUE_FALSE) question.options else question.options.shuffled()
        shownAt = clock.millis()
        mutableState.value = SessionUiState.Answering(
            index = index,
            total = ids.size,
            question = question,
            options = options,
            selected = emptySet(),
            checked = false,
            wasCorrect = null,
            flagged = toggleFlag.isFlagged(question.id),
            audioPlaying = false,
            isSample = request == PracticeRequest.Sample,
            sectionTitle = content.section(question.sectionId)?.title.orEmpty(),
        )
        val s = settings.current()
        if (s.audioAutoplay) readAloud.play(ReadAloudItem.QuestionItem(question, options), s.audioSpeed)
    }

    private suspend fun skip(index: Int) = if (index + 1 < ids.size) show(index + 1) else finish()

    fun select(optionId: String) = mutableState.update { s ->
        if (s !is SessionUiState.Answering || s.checked) return@update s
        val selected = when {
            s.question.type != QuestionType.MULTI -> setOf(optionId)
            optionId in s.selected -> s.selected - optionId
            s.selected.size < 2 -> s.selected + optionId
            else -> s.selected
        }
        s.copy(selected = selected)
    }

    fun check() {
        val s = mutableState.value as? SessionUiState.Answering ?: return
        if (!s.canCheck || s.checked) return
        readAloud.stop()
        viewModelScope.launch {
            val correct = recordAnswer(s.question, s.selected.toList(), mode, sessionId, clock.millis() - shownAt)
            results += s.question to correct
            mutableState.value = s.copy(checked = true, wasCorrect = correct, audioPlaying = false)
        }
    }

    fun next() {
        val s = mutableState.value as? SessionUiState.Answering ?: return
        readAloud.stop()
        viewModelScope.launch { if (s.isLast) finish() else show(s.index + 1) }
    }

    fun toggleFlag() {
        val s = mutableState.value as? SessionUiState.Answering ?: return
        viewModelScope.launch {
            toggleFlag(s.question.id, !s.flagged)
            mutableState.value = s.copy(flagged = !s.flagged)
        }
    }

    fun toggleAudio(explanation: Boolean) {
        val s = mutableState.value as? SessionUiState.Answering ?: return
        viewModelScope.launch {
            val item = if (explanation) ReadAloudItem.ExplanationItem(s.question) else ReadAloudItem.QuestionItem(s.question, s.options)
            readAloud.toggle(item, settings.current().audioSpeed)
        }
    }

    val audioPlaying = readAloud.playing

    fun report(reason: ReportReason, comment: String) {
        val s = mutableState.value as? SessionUiState.Answering ?: return
        viewModelScope.launch {
            progress.queueReport(
                QuestionReport(
                    questionId = s.question.id,
                    reason = reason,
                    comment = comment.trim(),
                    contentVersion = content.contentVersion(),
                    appVersion = appInfo.versionName,
                    createdAt = clock.instant(),
                ),
            )
            analytics.log(AnalyticsEvent.QuestionReported(s.question.id, reason.storageValue))
            // Works offline: queued and sent with the next sync.
            backupScheduler.requestBackup()
        }
    }

    private suspend fun finish() {
        readAloud.stop()
        val answered = results.size
        val correct = results.count { it.second }
        if (sessionId.isNotEmpty()) finishSession(sessionId, answered, correct)
        val durationS = (clock.millis() - startedAt) / 1000
        when (request) {
            PracticeRequest.Sample -> {
                settings.setSampleDone(true)
                analytics.log(AnalyticsEvent.SampleQuizComplete(correct))
            }
            PracticeRequest.Review -> analytics.log(AnalyticsEvent.ReviewSessionComplete(answered, correct))
            else -> analytics.log(AnalyticsEvent.PracticeSessionComplete(mode.dbValue, answered, correct, durationS))
        }
        val chapters = content.chapters().associateBy { it.id }
        val byChapter = results.groupBy { it.first.chapterId }.mapNotNull { (id, rs) ->
            chapters[id]?.let { ChapterResult(it.number, it.title, ChapterScore(rs.count { r -> r.second }, rs.size)) }
        }.sortedBy { it.number }
        val weakest = byChapter.filter { it.score.correct < it.score.total }
            .minWithOrNull(compareBy<ChapterResult> { it.score.correct.toDouble() / it.score.total }.thenByDescending { it.score.total - it.score.correct })
            ?.title
        val today = observeToday().first()
        mutableState.value = SessionUiState.Summary(
            answered = answered,
            correct = correct,
            missed = results.filterNot { it.second }.map { (q, _) ->
                MissedQuestion(q.stem, q.options.filter { it.isCorrect }.joinToString(" and ") { it.label })
            },
            todayAnswered = today.answered,
            goal = today.goal,
            streak = today.streak,
            isSample = request == PracticeRequest.Sample,
            chapterResults = byChapter,
            weakestChapter = weakest,
        )
    }

    override fun onCleared() {
        readAloud.stop()
    }
}
