package com.myday.litu.core.progress.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tables mirror spec section 8.2, plus pending_report for question reports queued offline (S11b).
// Schema changes are explicit Room migrations with tests; destructive migration is forbidden here.

@Entity(tableName = "attempt", indices = [Index(value = ["question_id", "answered_at"], name = "idx_attempt_question")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "question_id") val questionId: String,
    /** JSON array of option IDs. */
    val selected: String,
    val correct: Int,
    val mode: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "answered_at") val answeredAt: Long,
    @ColumnInfo(name = "time_ms") val timeMs: Long,
)

@Entity(tableName = "review_state", indices = [Index(value = ["due_at"], name = "idx_review_due")])
data class ReviewStateEntity(
    @PrimaryKey @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(defaultValue = "2.5") val ease: Double,
    @ColumnInfo(name = "interval_days", defaultValue = "0") val intervalDays: Int,
    @ColumnInfo(name = "due_at") val dueAt: Long,
    @ColumnInfo(defaultValue = "0") val reps: Int,
    @ColumnInfo(defaultValue = "0") val lapses: Int,
    @ColumnInfo(defaultValue = "0") val flagged: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "study_session")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val mode: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(defaultValue = "0") val answered: Int,
    @ColumnInfo(defaultValue = "0") val correct: Int,
    @ColumnInfo(defaultValue = "0") val synced: Int,
)

@Entity(tableName = "mock_exam")
data class MockExamEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    /** JSON array of 24 IDs, in order. */
    @ColumnInfo(name = "question_ids") val questionIds: String,
    /** JSON map questionId -> optionIds. */
    val answers: String,
    val score: Int?,
    val passed: Int?,
    /** JSON map chapterId -> [correct, total]. */
    @ColumnInfo(name = "chapter_breakdown") val chapterBreakdown: String?,
    @ColumnInfo(name = "content_version") val contentVersion: Int,
)

@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    /** 'YYYY-MM-DD' local date. */
    @PrimaryKey val day: String,
    @ColumnInfo(defaultValue = "0") val answered: Int,
    @ColumnInfo(defaultValue = "0") val correct: Int,
    @ColumnInfo(name = "study_minutes", defaultValue = "0") val studyMinutes: Int,
    @ColumnInfo(name = "goal_met", defaultValue = "0") val goalMet: Int,
)

@Entity(tableName = "pending_report")
data class PendingReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "question_id") val questionId: String,
    val reason: String,
    val comment: String,
    @ColumnInfo(name = "content_version") val contentVersion: Int,
    @ColumnInfo(name = "app_version") val appVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
