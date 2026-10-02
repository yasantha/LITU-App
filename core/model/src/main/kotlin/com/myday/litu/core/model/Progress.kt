package com.myday.litu.core.model

import java.time.Instant
import java.time.LocalDate

enum class StudyMode(val dbValue: String) {
    SAMPLE("sample"),
    PRACTICE("practice"),
    REVIEW("review"),
    MOCK("mock"),
    TIMER("timer");

    companion object {
        fun fromDb(value: String): StudyMode = entries.first { it.dbValue == value }
    }
}

data class Attempt(
    val questionId: String,
    val selected: List<String>,
    val correct: Boolean,
    val mode: StudyMode,
    val sessionId: String,
    val answeredAt: Instant,
    val timeMs: Long,
)

data class ReviewState(
    val questionId: String,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val dueAt: Instant,
    val reps: Int = 0,
    val lapses: Int = 0,
    val flagged: Boolean = false,
    val updatedAt: Instant,
)

data class StudySession(
    val id: String,
    val mode: StudyMode,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val answered: Int = 0,
    val correct: Int = 0,
    val synced: Boolean = false,
)

data class ChapterScore(val correct: Int, val total: Int)

data class MockExam(
    val id: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val questionIds: List<String>,
    val answers: Map<String, List<String>>,
    val score: Int? = null,
    val passed: Boolean? = null,
    val chapterBreakdown: Map<String, ChapterScore> = emptyMap(),
    val contentVersion: Int,
) {
    val isFinished: Boolean get() = finishedAt != null
}

data class DailyStat(
    val day: LocalDate,
    val answered: Int = 0,
    val correct: Int = 0,
    val studyMinutes: Int = 0,
    val goalMet: Boolean = false,
)

/** Per-section accuracy, used for weakest topics and mastery bars. */
data class SectionStat(
    val sectionId: String,
    val chapterId: String,
    val answered: Int,
    val correct: Int,
)
