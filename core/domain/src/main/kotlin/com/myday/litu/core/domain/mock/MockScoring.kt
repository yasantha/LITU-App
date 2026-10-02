package com.myday.litu.core.domain.mock

import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.Exam
import com.myday.litu.core.model.Question
import java.time.Duration
import java.time.Instant

data class MockResult(
    val score: Int,
    val total: Int,
    val passed: Boolean,
    val chapterBreakdown: Map<String, ChapterScore>,
) {
    val percent: Double get() = if (total == 0) 0.0 else score * 100.0 / total

    /** How many more correct answers were needed to pass; 0 when passed. */
    val shortBy: Int get() = (Exam.PASS_MARK - score).coerceAtLeast(0)
}

object MockScoring {
    fun score(questions: List<Question>, answers: Map<String, List<String>>): MockResult {
        val breakdown = questions.groupBy { it.chapterId }.mapValues { (_, qs) ->
            ChapterScore(correct = qs.count { it.isCorrect(answers[it.id].orEmpty()) }, total = qs.size)
        }
        val score = breakdown.values.sumOf { it.correct }
        return MockResult(score, questions.size, score >= Exam.PASS_MARK, breakdown)
    }

    val DURATION: Duration = Duration.ofMinutes(Exam.DURATION_MINUTES)

    /** Wall-clock countdown, so it keeps running when the app is closed. */
    fun remaining(startedAt: Instant, now: Instant): Duration =
        (DURATION - Duration.between(startedAt, now)).let { if (it.isNegative) Duration.ZERO else it }

    val WARNING_THRESHOLD: Duration = Duration.ofMinutes(5)
}
