package com.myday.litu.core.domain.repository

import com.myday.litu.core.domain.sync.BackupSnapshot
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.ReviewState
import com.myday.litu.core.model.StudyMode
import com.myday.litu.core.model.StudySession
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Read-write access to user.db. Never wiped by a content update. */
interface ProgressRepository {
    suspend fun recordAttempt(attempt: Attempt)
    suspend fun attemptsSince(since: Instant): List<Attempt>
    fun observeTotalAnswered(): Flow<Int>

    suspend fun reviewState(questionId: String): ReviewState?
    suspend fun upsertReviewState(state: ReviewState)
    fun observeReviewStates(): Flow<List<ReviewState>>
    suspend fun dueReviews(now: Instant, limit: Int): List<ReviewState>
    suspend fun upcomingReviews(now: Instant, limit: Int): List<ReviewState>

    suspend fun startSession(mode: StudyMode, now: Instant): StudySession
    suspend fun finishSession(id: String, answered: Int, correct: Int, now: Instant)
    suspend fun unsyncedSessionCount(): Int
    suspend fun markSessionsSynced()

    suspend fun saveMock(mock: MockExam)
    suspend fun mock(id: String): MockExam?
    fun observeFinishedMocks(): Flow<List<MockExam>>
    suspend fun recentFinishedMocks(limit: Int): List<MockExam>
    suspend fun unfinishedMock(): MockExam?
    suspend fun deleteMock(id: String)

    fun observeDailyStats(): Flow<List<DailyStat>>
    suspend fun dailyStat(day: LocalDate): DailyStat?
    suspend fun addToDailyStat(day: LocalDate, answered: Int, correct: Int, studyMinutes: Int, dailyGoal: Int)

    suspend fun queueReport(report: QuestionReport)
    suspend fun pendingReports(): List<QuestionReport>
    suspend fun removeReports(ids: List<Long>)

    suspend fun exportSnapshot(now: Instant): BackupSnapshot
    suspend fun importSnapshot(snapshot: BackupSnapshot)
    suspend fun deleteAll()
}

enum class ReportReason(val storageValue: String) {
    WRONG("wrong"),
    UNCLEAR("unclear"),
    TYPO("typo"),
    OTHER("other"),
}

data class QuestionReport(
    val id: Long = 0,
    val questionId: String,
    val reason: ReportReason,
    val comment: String,
    val contentVersion: Int,
    val appVersion: String,
    val createdAt: Instant,
) {
    companion object {
        const val MAX_COMMENT = 500
    }
}
