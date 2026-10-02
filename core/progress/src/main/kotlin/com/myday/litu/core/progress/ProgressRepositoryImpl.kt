package com.myday.litu.core.progress

import androidx.room.withTransaction
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.QuestionReport
import com.myday.litu.core.domain.repository.ReportReason
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.streak.Streaks
import com.myday.litu.core.domain.sync.BackupMerge
import com.myday.litu.core.domain.sync.BackupSettings
import com.myday.litu.core.domain.sync.BackupSnapshot
import com.myday.litu.core.domain.sync.MockSummary
import com.myday.litu.core.domain.sync.StreakSummary
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.ReviewState
import com.myday.litu.core.model.StudyMode
import com.myday.litu.core.model.StudySession
import com.myday.litu.core.progress.db.MockExamEntity
import com.myday.litu.core.progress.db.PendingReportEntity
import com.myday.litu.core.progress.db.ProgressDatabase
import com.myday.litu.core.progress.db.StudySessionEntity
import com.myday.litu.core.progress.db.toEntity
import com.myday.litu.core.progress.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val db: ProgressDatabase,
    private val settings: SettingsRepository,
) : ProgressRepository {
    private val dao = db.progressDao()

    override suspend fun recordAttempt(attempt: Attempt) = dao.insertAttempt(attempt.toEntity())

    override suspend fun attemptsSince(since: Instant): List<Attempt> =
        dao.attemptsSince(since.toEpochMilli()).map { it.toModel() }

    override fun observeTotalAnswered(): Flow<Int> = dao.observeAttemptCount()

    override suspend fun reviewState(questionId: String): ReviewState? = dao.reviewState(questionId)?.toModel()

    override suspend fun upsertReviewState(state: ReviewState) = dao.upsertReviewState(state.toEntity())

    override fun observeReviewStates(): Flow<List<ReviewState>> =
        dao.observeReviewStates().map { list -> list.map { it.toModel() } }

    override suspend fun dueReviews(now: Instant, limit: Int) = dao.dueReviews(now.toEpochMilli(), limit).map { it.toModel() }

    override suspend fun upcomingReviews(now: Instant, limit: Int) =
        dao.upcomingReviews(now.toEpochMilli(), limit).map { it.toModel() }

    override suspend fun startSession(mode: StudyMode, now: Instant): StudySession {
        val session = StudySession(UUID.randomUUID().toString(), mode, now)
        dao.insertSession(StudySessionEntity(session.id, mode.dbValue, now.toEpochMilli(), null, 0, 0, 0))
        return session
    }

    override suspend fun finishSession(id: String, answered: Int, correct: Int, now: Instant) =
        dao.finishSession(id, now.toEpochMilli(), answered, correct)

    override suspend fun unsyncedSessionCount(): Int = dao.unsyncedSessionCount()

    override suspend fun markSessionsSynced() = dao.markSessionsSynced()

    override suspend fun saveMock(mock: MockExam) = dao.upsertMock(mock.toEntity())

    override suspend fun mock(id: String): MockExam? = dao.mock(id)?.toModel()

    override fun observeFinishedMocks(): Flow<List<MockExam>> =
        dao.observeFinishedMocks().map { list -> list.map { it.toModel() } }

    override suspend fun recentFinishedMocks(limit: Int) = dao.recentFinishedMocks(limit).map { it.toModel() }

    override suspend fun unfinishedMock(): MockExam? = dao.unfinishedMock()?.toModel()

    override suspend fun deleteMock(id: String) = dao.deleteMock(id)

    override fun observeDailyStats(): Flow<List<DailyStat>> =
        dao.observeDailyStats().map { list -> list.map { it.toModel() } }

    override suspend fun dailyStat(day: LocalDate): DailyStat? = dao.dailyStat(day.toString())?.toModel()

    override suspend fun addToDailyStat(day: LocalDate, answered: Int, correct: Int, studyMinutes: Int, dailyGoal: Int) =
        dao.addToDailyStat(day.toString(), answered, correct, studyMinutes, dailyGoal)

    override suspend fun queueReport(report: QuestionReport) = dao.insertReport(
        PendingReportEntity(
            questionId = report.questionId,
            reason = report.reason.storageValue,
            comment = report.comment.take(QuestionReport.MAX_COMMENT),
            contentVersion = report.contentVersion,
            appVersion = report.appVersion,
            createdAt = report.createdAt.toEpochMilli(),
        ),
    )

    override suspend fun pendingReports(): List<QuestionReport> = dao.pendingReports().map {
        QuestionReport(
            id = it.id,
            questionId = it.questionId,
            reason = ReportReason.entries.first { r -> r.storageValue == it.reason },
            comment = it.comment,
            contentVersion = it.contentVersion,
            appVersion = it.appVersion,
            createdAt = Instant.ofEpochMilli(it.createdAt),
        )
    }

    override suspend fun removeReports(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteReports(ids)
    }

    override suspend fun exportSnapshot(now: Instant): BackupSnapshot {
        val s = settings.current()
        val stats = dao.dailyStats(BackupSnapshot.MAX_DAILY_STATS).map { it.toModel() }
        val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
        val streak = Streaks.compute(stats.filter { it.goalMet }.mapTo(HashSet()) { it.day }, today)
        return BackupSnapshot(
            updatedAt = now,
            settings = BackupSettings(s.testDate, s.dailyGoal, s.reminderTime),
            reviewStates = dao.reviewStates().map { it.toModel() },
            mocks = dao.recentFinishedMocks(BackupSnapshot.MAX_MOCKS).mapNotNull { e ->
                val m = e.toModel()
                MockSummary(m.id, m.finishedAt ?: return@mapNotNull null, m.score ?: 0, m.passed == true, m.chapterBreakdown)
            },
            dailyStats = stats,
            streak = StreakSummary(streak.current, streak.best, stats.firstOrNull { it.goalMet }?.day),
        )
    }

    override suspend fun importSnapshot(snapshot: BackupSnapshot) {
        val merged = BackupMerge.merge(exportSnapshot(snapshot.updatedAt), snapshot)
        db.withTransaction {
            dao.upsertReviewStates(merged.reviewStates.map { it.toEntity() })
            // Restored mocks keep their score and breakdown; their answers stay on the old phone.
            dao.insertMocksIfAbsent(
                merged.mocks.map { m ->
                    MockExamEntity(
                        id = m.id,
                        startedAt = m.finishedAt.toEpochMilli(),
                        finishedAt = m.finishedAt.toEpochMilli(),
                        questionIds = "[]",
                        answers = "{}",
                        score = m.score,
                        passed = if (m.passed) 1 else 0,
                        chapterBreakdown = com.myday.litu.core.progress.db.progressJson.encodeToString(
                            m.breakdown.mapValues { listOf(it.value.correct, it.value.total) },
                        ),
                        contentVersion = 0,
                    )
                },
            )
            merged.dailyStats.forEach { dao.upsertDailyStat(it.toEntity()) }
        }
        settings.setTestDate(merged.settings.testDate)
        settings.setDailyGoal(merged.settings.dailyGoal)
        val current = settings.current()
        settings.setReminder(merged.settings.reminderTime, current.remindersEnabled)
    }

    override suspend fun deleteAll() = db.clearAllTables()
}
