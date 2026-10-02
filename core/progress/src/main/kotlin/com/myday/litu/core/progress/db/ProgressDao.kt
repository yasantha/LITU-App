package com.myday.litu.core.progress.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempt WHERE answered_at >= :since ORDER BY answered_at")
    suspend fun attemptsSince(since: Long): List<AttemptEntity>

    @Query("SELECT COUNT(*) FROM attempt")
    fun observeAttemptCount(): Flow<Int>

    @Query("SELECT * FROM review_state WHERE question_id = :questionId")
    suspend fun reviewState(questionId: String): ReviewStateEntity?

    @Upsert
    suspend fun upsertReviewState(state: ReviewStateEntity)

    @Upsert
    suspend fun upsertReviewStates(states: List<ReviewStateEntity>)

    @Query("SELECT * FROM review_state")
    fun observeReviewStates(): Flow<List<ReviewStateEntity>>

    @Query("SELECT * FROM review_state")
    suspend fun reviewStates(): List<ReviewStateEntity>

    @Query("SELECT * FROM review_state WHERE due_at <= :now ORDER BY due_at LIMIT :limit")
    suspend fun dueReviews(now: Long, limit: Int): List<ReviewStateEntity>

    @Query("SELECT * FROM review_state WHERE due_at > :now ORDER BY due_at LIMIT :limit")
    suspend fun upcomingReviews(now: Long, limit: Int): List<ReviewStateEntity>

    @Insert
    suspend fun insertSession(session: StudySessionEntity)

    @Query("UPDATE study_session SET ended_at = :endedAt, answered = :answered, correct = :correct WHERE id = :id")
    suspend fun finishSession(id: String, endedAt: Long, answered: Int, correct: Int)

    @Query("SELECT COUNT(*) FROM study_session WHERE synced = 0 AND ended_at IS NOT NULL")
    suspend fun unsyncedSessionCount(): Int

    @Query("UPDATE study_session SET synced = 1 WHERE ended_at IS NOT NULL")
    suspend fun markSessionsSynced()

    @Upsert
    suspend fun upsertMock(mock: MockExamEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMocksIfAbsent(mocks: List<MockExamEntity>)

    @Query("SELECT * FROM mock_exam WHERE id = :id")
    suspend fun mock(id: String): MockExamEntity?

    @Query("SELECT * FROM mock_exam WHERE finished_at IS NOT NULL ORDER BY finished_at DESC")
    fun observeFinishedMocks(): Flow<List<MockExamEntity>>

    @Query("SELECT * FROM mock_exam WHERE finished_at IS NOT NULL ORDER BY finished_at DESC LIMIT :limit")
    suspend fun recentFinishedMocks(limit: Int): List<MockExamEntity>

    @Query("SELECT * FROM mock_exam WHERE finished_at IS NULL ORDER BY started_at DESC LIMIT 1")
    suspend fun unfinishedMock(): MockExamEntity?

    @Query("DELETE FROM mock_exam WHERE id = :id")
    suspend fun deleteMock(id: String)

    @Query("SELECT * FROM daily_stat ORDER BY day DESC")
    fun observeDailyStats(): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stat ORDER BY day DESC LIMIT :limit")
    suspend fun dailyStats(limit: Int): List<DailyStatEntity>

    @Query("SELECT * FROM daily_stat WHERE day = :day")
    suspend fun dailyStat(day: String): DailyStatEntity?

    @Upsert
    suspend fun upsertDailyStat(stat: DailyStatEntity)

    @Transaction
    suspend fun addToDailyStat(day: String, answered: Int, correct: Int, minutes: Int, goal: Int) {
        val old = dailyStat(day) ?: DailyStatEntity(day, 0, 0, 0, 0)
        val total = old.answered + answered
        upsertDailyStat(
            old.copy(
                answered = total,
                correct = old.correct + correct,
                studyMinutes = old.studyMinutes + minutes,
                // Once met, a day stays counted even if the goal is raised later.
                goalMet = if (old.goalMet == 1 || total >= goal) 1 else 0,
            ),
        )
    }

    @Insert
    suspend fun insertReport(report: PendingReportEntity)

    @Query("SELECT * FROM pending_report ORDER BY created_at")
    suspend fun pendingReports(): List<PendingReportEntity>

    @Query("DELETE FROM pending_report WHERE id IN (:ids)")
    suspend fun deleteReports(ids: List<Long>)
}
