package com.myday.litu.feature.mock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myday.litu.core.analytics.AnalyticsEvent
import com.myday.litu.core.analytics.AnalyticsLogger
import com.myday.litu.core.domain.mock.MockScoring
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.FinishMockUseCase
import com.myday.litu.core.domain.usecase.ToggleFlagUseCase
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.random.Random

data class LiveMockState(
    val mock: MockExam? = null,
    val index: Int = 0,
    val question: Question? = null,
    val options: List<AnswerOption> = emptyList(),
    val remaining: Duration = MockScoring.DURATION,
    val flagged: Set<String> = emptySet(),
    val finishedId: String? = null,
) {
    val total: Int get() = mock?.questionIds?.size ?: 0
    val selected: Set<String> get() = question?.let { mock?.answers?.get(it.id)?.toSet() }.orEmpty()
    val unanswered: Int get() = mock?.let { m -> m.questionIds.count { m.answers[it].isNullOrEmpty() } } ?: 0
}

/** S15: exam conditions, no feedback until the end, wall-clock timer that auto-submits at 00:00. */
@HiltViewModel
class LiveMockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val finishMock: FinishMockUseCase,
    private val toggleFlag: ToggleFlagUseCase,
    private val analytics: AnalyticsLogger,
    private val clock: Clock,
) : ViewModel() {
    private val mockId = savedStateHandle.toRoute<LiveMockRoute>().mockId
    private val mutableState = MutableStateFlow(LiveMockState())
    val state = mutableState.asStateFlow()
    private var finishing = false

    init {
        viewModelScope.launch {
            val mock = progress.mock(mockId) ?: return@launch
            if (mock.isFinished) {
                mutableState.update { it.copy(finishedId = mockId) }
                return@launch
            }
            val flagged = mock.questionIds.filter { toggleFlag.isFlagged(it) }.toSet()
            mutableState.update { it.copy(mock = mock, flagged = flagged) }
            show(0)
            tick()
        }
    }

    private suspend fun tick() {
        while (viewModelScope.isActive) {
            val m = mutableState.value.mock ?: return
            val remaining = MockScoring.remaining(m.startedAt, clock.instant())
            mutableState.update { it.copy(remaining = remaining) }
            if (remaining.isZero) return finish()
            delay(1_000 - clock.millis() % 1_000)
        }
    }

    fun show(index: Int) = viewModelScope.launch {
        val m = mutableState.value.mock ?: return@launch
        val id = m.questionIds.getOrNull(index) ?: return@launch
        val q = content.question(id) ?: content.questions(listOf(id)).firstOrNull()
        // A stable shuffle per mock, so options keep their order when moving back and forth.
        val options = when {
            q == null -> emptyList()
            q.type == QuestionType.TRUE_FALSE -> q.options
            else -> q.options.shuffled(Random((mockId + id).hashCode()))
        }
        mutableState.update { it.copy(index = index, question = q, options = options) }
    }

    fun select(optionId: String) {
        val s = mutableState.value
        val q = s.question ?: return
        val m = s.mock ?: return
        val current = s.selected
        val next = when {
            q.type != QuestionType.MULTI -> setOf(optionId)
            optionId in current -> current - optionId
            current.size < 2 -> current + optionId
            else -> current
        }
        val updated = m.copy(answers = m.answers + (q.id to next.toList()))
        mutableState.update { it.copy(mock = updated) }
        viewModelScope.launch { progress.saveMock(updated) }
    }

    fun toggleFlag() {
        val q = mutableState.value.question ?: return
        val flagged = q.id !in mutableState.value.flagged
        mutableState.update { it.copy(flagged = if (flagged) it.flagged + q.id else it.flagged - q.id) }
        viewModelScope.launch { toggleFlag(q.id, flagged) }
    }

    fun next() = show(mutableState.value.index + 1)
    fun previous() = show(mutableState.value.index - 1)

    fun submit() = viewModelScope.launch { finish() }

    private suspend fun finish() {
        if (finishing) return
        finishing = true
        val m = mutableState.value.mock ?: return
        progress.saveMock(m)
        val result = finishMock(mockId)
        val duration = Duration.between(m.startedAt, clock.instant()).coerceAtMost(MockScoring.DURATION)
        analytics.log(AnalyticsEvent.MockComplete(result.score, result.passed, duration.seconds))
        mutableState.update { it.copy(finishedId = mockId) }
    }
}
