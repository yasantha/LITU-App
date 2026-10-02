package com.myday.litu.core.domain.readiness

import kotlin.math.roundToInt

enum class ReadinessLevel {
    /** 0–59%: "Keep practising", error colour. */
    KEEP_PRACTISING,

    /** 60–79%: "Getting close", warning colour. */
    GETTING_CLOSE,

    /** 80–100%: "Likely to pass", success colour. */
    LIKELY_TO_PASS;

    companion object {
        fun fromScore(score: Int): ReadinessLevel = when {
            score >= 80 -> LIKELY_TO_PASS
            score >= 60 -> GETTING_CLOSE
            else -> KEEP_PRACTISING
        }
    }
}

/**
 * @param share the chapter's share of the test (weights sum to 1 across test chapters).
 * @param activeQuestions active questions in the chapter.
 * @param correctRecently distinct questions answered correctly in the last 30 days.
 */
data class ChapterCoverage(
    val chapterId: String,
    val share: Double,
    val activeQuestions: Int,
    val correctRecently: Int,
) {
    val mastery: Double
        get() = if (activeQuestions == 0) 0.0 else (correctRecently.toDouble() / activeQuestions).coerceIn(0.0, 1.0)
}

/** Readiness score (spec section 9.2). */
object Readiness {
    val MOCK_WEIGHTS = listOf(0.5, 0.3, 0.2)
    const val MOCK_FACTOR = 0.65
    const val COVERAGE_FACTOR = 0.35
    const val COVERAGE_WINDOW_DAYS = 30L

    /** Weighted average of the last 3 mock scores in %, newest first; weights re-normalised. */
    fun mockPart(recentScoresPercent: List<Double>): Double? {
        val scores = recentScoresPercent.take(MOCK_WEIGHTS.size)
        if (scores.isEmpty()) return null
        val weights = MOCK_WEIGHTS.take(scores.size)
        return scores.zip(weights).sumOf { (s, w) -> s * w } / weights.sum()
    }

    /** Σ chapterShare × chapterMastery, in %. */
    fun coveragePart(chapters: List<ChapterCoverage>): Double {
        val totalShare = chapters.sumOf { it.share }
        if (totalShare <= 0.0) return 0.0
        return chapters.sumOf { (it.share / totalShare) * it.mastery } * 100.0
    }

    /** Null until the user has completed at least one mock. */
    fun score(recentScoresPercent: List<Double>, chapters: List<ChapterCoverage>): Int? {
        val mock = mockPart(recentScoresPercent) ?: return null
        return (MOCK_FACTOR * mock + COVERAGE_FACTOR * coveragePart(chapters)).roundToInt().coerceIn(0, 100)
    }

    /** Shares proportional to each chapter's active question count. */
    fun proportionalShares(activeByChapter: Map<String, Int>): Map<String, Double> {
        val total = activeByChapter.values.sum().toDouble()
        if (total == 0.0) return activeByChapter.mapValues { 0.0 }
        return activeByChapter.mapValues { it.value / total }
    }
}
