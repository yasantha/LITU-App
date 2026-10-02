package com.myday.litu.core.domain

import com.myday.litu.core.domain.sync.BackupMerge
import com.myday.litu.core.domain.sync.BackupSettings
import com.myday.litu.core.domain.sync.BackupSnapshot
import com.myday.litu.core.domain.sync.MockSummary
import com.myday.litu.core.domain.sync.StreakSummary
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupMergeTest {
    private val t0 = Instant.parse("2026-10-01T10:00:00Z")
    private fun snapshot(at: Instant, goal: Int, reviews: List<ReviewState>, mocks: List<String>, stats: List<DailyStat>, best: Int) =
        BackupSnapshot(
            updatedAt = at,
            settings = BackupSettings(null, goal, LocalTime.of(19, 0)),
            reviewStates = reviews,
            mocks = mocks.map { MockSummary(it, at, 20, true, emptyMap()) },
            dailyStats = stats,
            streak = StreakSummary(1, best, null),
        )

    @Test fun latestReviewStateWinsAndCollectionsAreUnioned() {
        val day = LocalDate.of(2026, 10, 1)
        val local = snapshot(
            t0, 10,
            listOf(ReviewState("A", dueAt = t0, reps = 1, updatedAt = t0), ReviewState("B", dueAt = t0, updatedAt = t0.plusSeconds(60))),
            listOf("m1"), listOf(DailyStat(day, answered = 5)), best = 4,
        )
        val remote = snapshot(
            t0.plusSeconds(30), 40,
            listOf(ReviewState("A", dueAt = t0, reps = 2, updatedAt = t0.plusSeconds(30)), ReviewState("B", dueAt = t0, reps = 9, updatedAt = t0)),
            listOf("m1", "m2"), listOf(DailyStat(day, answered = 12), DailyStat(day.minusDays(1), answered = 3)), best = 2,
        )
        val merged = BackupMerge.merge(local, remote)
        assertEquals(2, merged.reviewStates.first { it.questionId == "A" }.reps)
        assertEquals(0, merged.reviewStates.first { it.questionId == "B" }.reps)
        assertEquals(setOf("m1", "m2"), merged.mocks.map { it.id }.toSet())
        assertEquals(12, merged.dailyStats.first { it.day == day }.answered)
        assertEquals(2, merged.dailyStats.size)
        assertEquals(40, merged.settings.dailyGoal)
        assertEquals(4, merged.streak.best)
    }
}
