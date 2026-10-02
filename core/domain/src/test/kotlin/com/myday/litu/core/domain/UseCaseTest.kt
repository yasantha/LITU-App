package com.myday.litu.core.domain

import com.myday.litu.core.domain.repository.BackupScheduler
import com.myday.litu.core.domain.usecase.BuildPracticeSessionUseCase
import com.myday.litu.core.domain.usecase.FinishMockUseCase
import com.myday.litu.core.domain.usecase.PracticeRequest
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.domain.usecase.RecordAnswerUseCase
import com.myday.litu.core.domain.usecase.ReviewUpdater
import com.myday.litu.core.domain.usecase.StartMockUseCase
import com.myday.litu.core.domain.usecase.ToggleFlagUseCase
import com.myday.litu.core.model.StudyMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class UseCaseTest {
    private val now = Instant.parse("2026-10-01T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val questions = (1..30).map { question("Q$it", chapter = if (it <= 15) "CH3" else "CH5") } +
        (1..5).map { question("V$it", chapter = "CH1") }
    private val content = FakeContent(questions)
    private val progress = FakeProgress()
    private val settings = FakeSettings()
    private var backups = 0
    private val scheduler = BackupScheduler { backups++ }
    private val updater = ReviewUpdater(progress, settings, clock)
    private val record = RecordAnswerUseCase(progress, settings, updater, clock)

    @Test fun correctFirstAnswerCreatesNoReviewRow() = runTest {
        assertTrue(record(questions[0], listOf("Q1-A"), StudyMode.PRACTICE, "s", 1000))
        assertNull(progress.reviewState("Q1"))
        assertEquals(1, progress.attempts.size)
    }

    @Test fun wrongAnswerSchedulesReviewTomorrowThenCorrectAdvances() = runTest {
        assertFalse(record(questions[0], listOf("Q1-B"), StudyMode.PRACTICE, "s", 1000))
        val row = progress.reviewState("Q1")!!
        assertEquals(now.plus(1, ChronoUnit.DAYS), row.dueAt)
        record(questions[0], listOf("Q1-A"), StudyMode.REVIEW, "s", 1000)
        assertEquals(1, progress.reviewState("Q1")!!.reps)
    }

    @Test fun dailyGoalIsMetWhenAnsweredReachesGoal() = runTest {
        settings.setDailyGoal(10)
        repeat(10) { record(questions[it], listOf("Q${it + 1}-A"), StudyMode.PRACTICE, "s", 1000) }
        assertTrue(progress.stats.value.values.single().goalMet)
    }

    @Test fun flaggingCreatesDueRowAndUnflagKeepsIt() = runTest {
        val flag = ToggleFlagUseCase(progress, clock)
        flag("Q2", true)
        assertTrue(flag.isFlagged("Q2"))
        assertEquals(now, progress.reviewState("Q2")!!.dueAt)
        flag("Q2", false)
        assertFalse(flag.isFlagged("Q2"))
        assertNotNull(progress.reviewState("Q2"))
    }

    @Test fun mockUsesTestChaptersOnlyAndFinishScoresAndQueuesWrongAnswers() = runTest {
        val start = StartMockUseCase(content, progress, FakeConfig(), clock).apply { random = Random(3) }
        val id = start()
        val mock = progress.mock(id)!!
        assertEquals(24, mock.questionIds.size)
        assertTrue(mock.questionIds.none { it.startsWith("V") })
        // Resuming within 45 minutes returns the same mock.
        assertEquals(id, start())

        val answers = mock.questionIds.mapIndexed { i, q -> q to listOf(if (i < 18) "$q-A" else "$q-B") }.toMap()
        progress.saveMock(mock.copy(answers = answers))
        val result = FinishMockUseCase(content, progress, settings, updater, scheduler, clock)(id)
        assertEquals(18, result.score)
        assertTrue(result.passed)
        assertTrue(progress.mock(id)!!.isFinished)
        assertEquals(6, progress.reviews.value.size)
        assertEquals(24, progress.attempts.size)
        assertEquals(1, backups)
    }

    @Test fun expiredMockIsAutoSubmitted() = runTest {
        val id = StartMockUseCase(content, progress, FakeConfig(), clock)()
        val later = Clock.fixed(now.plusSeconds(46 * 60), ZoneOffset.UTC)
        val finish = FinishMockUseCase(content, progress, settings, ReviewUpdater(progress, settings, later), scheduler, later)
        assertEquals(id, finish.finishExpired())
        assertEquals(now.plusSeconds(45 * 60), progress.mock(id)!!.finishedAt)
    }

    @Test fun reviewQueueIsDueOldestFirstCappedAndIgnoresOrphans() = runTest {
        val flag = ToggleFlagUseCase(progress, clock)
        flag("UNKNOWN", true)
        (1..30).forEach { flag("Q$it", true) }
        val build = BuildPracticeSessionUseCase(content, progress, Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC))
        val queue = build(PracticeRequest.Review)
        assertEquals(30, queue.size)
        assertFalse("UNKNOWN" in queue)
    }

    @Test fun readinessNeedsAMock() = runTest {
        val overview = ProgressOverviewUseCase(content, progress, FakeConfig(), clock)
        assertNull(overview().readiness)
        val id = StartMockUseCase(content, progress, FakeConfig(), clock)()
        val mock = progress.mock(id)!!
        progress.saveMock(mock.copy(answers = mock.questionIds.associateWith { listOf("$it-A") }))
        FinishMockUseCase(content, progress, settings, updater, scheduler, clock)(id)
        // 100% mock, 24 of 30 test questions correct: round(0.65 × 100 + 0.35 × 80) = 93
        assertEquals(93, overview().readiness)
    }
}
