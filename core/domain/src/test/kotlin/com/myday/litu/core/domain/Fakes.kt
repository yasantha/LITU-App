package com.myday.litu.core.domain

import com.myday.litu.core.domain.repository.AppConfig
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.QuestionReport
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.sync.BackupSnapshot
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.Note
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionRef
import com.myday.litu.core.model.QuestionType
import com.myday.litu.core.model.ReviewState
import com.myday.litu.core.model.Section
import com.myday.litu.core.model.StudyMode
import com.myday.litu.core.model.StudySession
import com.myday.litu.core.model.ThemePreference
import com.myday.litu.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun question(id: String, chapter: String = "CH3", correct: Set<String> = setOf("$id-A"), type: QuestionType = QuestionType.SINGLE) =
    Question(id, "$chapter-S", chapter, type, "Stem $id?", "Because.", "Chapter", 1, null,
        listOf("A", "B", "C", "D").mapIndexed { i, l -> AnswerOption("$id-$l", l, "$id-$l" in correct, i) })

class FakeContent(private val qs: List<Question>, private val chapters: List<Chapter> = listOf(
    Chapter("CH1", 1, "Values", false), Chapter("CH3", 3, "History", true), Chapter("CH5", 5, "Government", true),
)) : ContentRepository {
    override suspend fun contentVersion() = 1
    override suspend fun chapters() = chapters
    override suspend fun chapter(id: String) = chapters.firstOrNull { it.id == id }
    override suspend fun sections(chapterId: String) = allSections().filter { it.chapterId == chapterId }
    override suspend fun allSections() = chapters.map { Section("${it.id}-S", it.id, it.title, 0) }
    override suspend fun section(id: String) = allSections().firstOrNull { it.id == id }
    override suspend fun question(id: String) = qs.firstOrNull { it.id == id }
    override suspend fun questions(ids: List<String>) = ids.mapNotNull { id -> qs.firstOrNull { it.id == id } }
    override suspend fun questionRefs() = qs.map { QuestionRef(it.id, it.sectionId, it.chapterId) }
    override suspend fun questionRefsForChapter(chapterId: String) = questionRefs().filter { it.chapterId == chapterId }
    override suspend fun questionRefsForSection(sectionId: String) = questionRefs().filter { it.sectionId == sectionId }
    override suspend fun sampleQuestionIds() = qs.take(10).map { it.id }
    override suspend fun note(sectionId: String): Note? = null
}

class FakeProgress : ProgressRepository {
    val attempts = mutableListOf<Attempt>()
    val reviews = MutableStateFlow<Map<String, ReviewState>>(emptyMap())
    val mocks = MutableStateFlow<Map<String, MockExam>>(emptyMap())
    val stats = MutableStateFlow<Map<LocalDate, DailyStat>>(emptyMap())
    val reports = mutableListOf<QuestionReport>()

    override suspend fun recordAttempt(attempt: Attempt) { attempts += attempt }
    override suspend fun attemptsSince(since: Instant) = attempts.filter { it.answeredAt >= since }
    override fun observeTotalAnswered(): Flow<Int> = MutableStateFlow(attempts.size)
    override suspend fun reviewState(questionId: String) = reviews.value[questionId]
    override suspend fun upsertReviewState(state: ReviewState) { reviews.value = reviews.value + (state.questionId to state) }
    override fun observeReviewStates() = reviews.map { it.values.toList() }
    override suspend fun dueReviews(now: Instant, limit: Int) = reviews.value.values.filter { it.dueAt <= now }.sortedBy { it.dueAt }.take(limit)
    override suspend fun upcomingReviews(now: Instant, limit: Int) = reviews.value.values.filter { it.dueAt > now }.sortedBy { it.dueAt }.take(limit)
    override suspend fun startSession(mode: StudyMode, now: Instant) = StudySession("s1", mode, now)
    override suspend fun finishSession(id: String, answered: Int, correct: Int, now: Instant) = Unit
    override suspend fun unsyncedSessionCount() = 0
    override suspend fun markSessionsSynced() = Unit
    override suspend fun saveMock(mock: MockExam) { mocks.value = mocks.value + (mock.id to mock) }
    override suspend fun mock(id: String) = mocks.value[id]
    override fun observeFinishedMocks() = mocks.map { m -> m.values.filter { it.isFinished }.sortedByDescending { it.finishedAt } }
    override suspend fun recentFinishedMocks(limit: Int) = mocks.value.values.filter { it.isFinished }.sortedByDescending { it.finishedAt }.take(limit)
    override suspend fun unfinishedMock() = mocks.value.values.firstOrNull { !it.isFinished }
    override suspend fun deleteMock(id: String) { mocks.value = mocks.value - id }
    override fun observeDailyStats() = stats.map { it.values.sortedByDescending { s -> s.day } }
    override suspend fun dailyStat(day: LocalDate) = stats.value[day]
    override suspend fun addToDailyStat(day: LocalDate, answered: Int, correct: Int, studyMinutes: Int, dailyGoal: Int) {
        val old = stats.value[day] ?: DailyStat(day)
        val total = old.answered + answered
        stats.value = stats.value + (day to old.copy(answered = total, correct = old.correct + correct,
            studyMinutes = old.studyMinutes + studyMinutes, goalMet = old.goalMet || total >= dailyGoal))
    }
    override suspend fun queueReport(report: QuestionReport) { reports += report }
    override suspend fun pendingReports() = reports.toList()
    override suspend fun removeReports(ids: List<Long>) { reports.removeAll { it.id in ids } }
    override suspend fun exportSnapshot(now: Instant): BackupSnapshot = error("not used")
    override suspend fun importSnapshot(snapshot: BackupSnapshot) = Unit
    override suspend fun deleteAll() = Unit
}

class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state
    override suspend fun current() = state.value
    override suspend fun setTestDate(date: LocalDate?) { state.value = state.value.copy(testDate = date) }
    override suspend fun setDailyGoal(goal: Int) { state.value = state.value.copy(dailyGoal = goal) }
    override suspend fun setReminder(time: LocalTime, enabled: Boolean) { state.value = state.value.copy(reminderTime = time, remindersEnabled = enabled) }
    override suspend fun setTheme(theme: ThemePreference) { state.value = state.value.copy(theme = theme) }
    override suspend fun setAudioAutoplay(enabled: Boolean) { state.value = state.value.copy(audioAutoplay = enabled) }
    override suspend fun setAudioSpeed(speed: Float) { state.value = state.value.copy(audioSpeed = speed) }
    override suspend fun setOnboardingDone(done: Boolean) { state.value = state.value.copy(onboardingDone = done) }
    override suspend fun setSampleDone(done: Boolean) { state.value = state.value.copy(sampleDone = done) }
    override suspend fun setLastContentVersion(version: Int) { state.value = state.value.copy(lastContentVersion = version) }
    override suspend fun clear() { state.value = UserSettings() }
}

class FakeConfig(config: AppConfig = AppConfig()) : ConfigRepository {
    override val config: StateFlow<AppConfig> = MutableStateFlow(config)
    override suspend fun refresh() = Unit
}
