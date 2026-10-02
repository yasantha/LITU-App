package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.mock.MockBuilder
import com.myday.litu.core.domain.mock.MockResult
import com.myday.litu.core.domain.mock.MockScoring
import com.myday.litu.core.domain.repository.BackupScheduler
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.StudyMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class StartMockUseCase @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val config: ConfigRepository,
    private val clock: Clock,
) {
    var random: Random = Random.Default

    /** Resumes a mock still within its 45 minutes, otherwise builds a new one. Returns its ID. */
    suspend operator fun invoke(): String {
        val now = clock.instant()
        progress.unfinishedMock()?.let { open ->
            if (!MockScoring.remaining(open.startedAt, now).isZero) return open.id
        }
        val testChapters = content.chapters().filter { it.inTest }.mapTo(HashSet()) { it.id }
        val pool = content.questionRefs().filter { it.chapterId in testChapters }
        val recentlyUsed = progress.recentFinishedMocks(RECENT_MOCKS).flatMapTo(HashSet()) { it.questionIds }
        val ids = MockBuilder(random).build(pool, recentlyUsed, config.config.value.mockChapterWeights)
        val mock = MockExam(
            id = UUID.randomUUID().toString(),
            startedAt = now,
            questionIds = ids,
            answers = emptyMap(),
            contentVersion = content.contentVersion(),
        )
        progress.saveMock(mock)
        return mock.id
    }

    companion object {
        const val RECENT_MOCKS = 3
    }
}

class FinishMockUseCase @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val reviewUpdater: ReviewUpdater,
    private val backupScheduler: BackupScheduler,
    private val clock: Clock,
) {
    /** Scores and stores the mock, records each answer and adds wrong answers to the review queue. */
    suspend operator fun invoke(mockId: String): MockResult {
        val mock = requireNotNull(progress.mock(mockId)) { "Unknown mock $mockId" }
        val questions = content.questions(mock.questionIds)
        val result = MockScoring.score(questions, mock.answers)
        if (mock.isFinished) return result

        val now = clock.instant()
        val deadline = mock.startedAt.plus(MockScoring.DURATION)
        val finishedAt = if (now.isAfter(deadline)) deadline else now
        progress.saveMock(
            mock.copy(
                finishedAt = finishedAt,
                score = result.score,
                passed = result.passed,
                chapterBreakdown = result.chapterBreakdown,
            ),
        )
        var answered = 0
        var correct = 0
        val perQuestionMs = (finishedAt.toEpochMilli() - mock.startedAt.toEpochMilli()) / questions.size.coerceAtLeast(1)
        for (q in questions) {
            val selected = mock.answers[q.id].orEmpty()
            val isCorrect = q.isCorrect(selected)
            if (selected.isNotEmpty()) {
                answered++
                progress.recordAttempt(Attempt(q.id, selected, isCorrect, StudyMode.MOCK, mock.id, finishedAt, perQuestionMs))
            }
            if (isCorrect) correct++
            reviewUpdater.onAnswered(q.id, isCorrect, finishedAt)
        }
        val minutes = ((finishedAt.toEpochMilli() - mock.startedAt.toEpochMilli()) / 60_000).toInt()
        progress.addToDailyStat(LocalDate.now(clock), answered, correct, minutes, settings.current().dailyGoal)
        backupScheduler.requestBackup()
        return result
    }

    /** Auto-submits any mock whose 45 minutes ran out while the app was closed. */
    suspend fun finishExpired(now: Instant = clock.instant()): String? {
        val open = progress.unfinishedMock() ?: return null
        if (!MockScoring.remaining(open.startedAt, now).isZero) return null
        invoke(open.id)
        return open.id
    }
}
