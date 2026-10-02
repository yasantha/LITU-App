package com.myday.litu.core.domain.sync

import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.ReviewState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The state needed to restore progress; mirrors the users/{uid} document (spec section 12.3). */
data class BackupSnapshot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val updatedAt: Instant,
    val settings: BackupSettings,
    val reviewStates: List<ReviewState>,
    val mocks: List<MockSummary>,
    val dailyStats: List<DailyStat>,
    val streak: StreakSummary,
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_MOCKS = 30
        const val MAX_DAILY_STATS = 120
    }
}

data class BackupSettings(val testDate: LocalDate?, val dailyGoal: Int, val reminderTime: LocalTime)

data class MockSummary(
    val id: String,
    val finishedAt: Instant,
    val score: Int,
    val passed: Boolean,
    val breakdown: Map<String, ChapterScore>,
)

data class StreakSummary(val current: Int, val best: Int, val lastDay: LocalDate?)

object BackupMerge {
    /**
     * Review state merges per question by updatedAt (latest wins); mocks and daily stats are unioned
     * by ID or day. Settings come from the newer snapshot.
     */
    fun merge(local: BackupSnapshot, remote: BackupSnapshot): BackupSnapshot {
        val reviews = (local.reviewStates + remote.reviewStates)
            .groupBy { it.questionId }
            .map { (_, states) -> states.maxBy { it.updatedAt } }
            .sortedBy { it.questionId }
        val mocks = (local.mocks + remote.mocks).distinctBy { it.id }
            .sortedByDescending { it.finishedAt }
            .take(BackupSnapshot.MAX_MOCKS)
        val stats = (local.dailyStats + remote.dailyStats)
            .groupBy { it.day }
            .map { (_, days) -> days.maxWith(compareBy<DailyStat> { it.answered }.thenBy { it.studyMinutes }) }
            .sortedByDescending { it.day }
            .take(BackupSnapshot.MAX_DAILY_STATS)
        val newer = if (remote.updatedAt.isAfter(local.updatedAt)) remote else local
        return BackupSnapshot(
            updatedAt = maxOf(local.updatedAt, remote.updatedAt),
            settings = newer.settings,
            reviewStates = reviews,
            mocks = mocks,
            dailyStats = stats,
            streak = StreakSummary(
                current = newer.streak.current,
                best = maxOf(local.streak.best, remote.streak.best),
                lastDay = listOfNotNull(local.streak.lastDay, remote.streak.lastDay).maxOrNull(),
            ),
        )
    }
}
