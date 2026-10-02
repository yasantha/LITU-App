package com.myday.litu.core.domain.scheduling

import com.myday.litu.core.model.ReviewState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Simplified SM-2 (spec section 9.1). */
object SpacedRepetition {
    const val DEFAULT_MAX_INTERVAL_DAYS = 60
    const val MIN_EASE = 1.3
    const val MAX_EASE = 2.8
    const val MASTERED_REPS = 3
    const val MASTERED_INTERVAL_DAYS = 7

    /**
     * @param daysUntilTest days left until the user's test, or null when no date is set.
     *   Intervals never go past the test.
     */
    fun schedule(state: ReviewState, correct: Boolean, now: Instant, daysUntilTest: Int?): ReviewState =
        if (!correct) {
            state.copy(
                ease = max(MIN_EASE, state.ease - 0.2),
                intervalDays = 1,
                lapses = state.lapses + 1,
                reps = 0,
                dueAt = now.plus(1, ChronoUnit.DAYS),
                updatedAt = now,
            )
        } else {
            val next = when (state.reps) {
                0 -> 1
                1 -> 3
                2 -> 7
                else -> ceil(state.intervalDays * state.ease).toInt()
            }.coerceAtMost(daysUntilTest ?: DEFAULT_MAX_INTERVAL_DAYS)
            state.copy(
                ease = min(MAX_EASE, state.ease + 0.05),
                reps = state.reps + 1,
                intervalDays = next,
                dueAt = now.plus(next.toLong(), ChronoUnit.DAYS),
                updatedAt = now,
            )
        }

    /** A row is created the first time a question is answered wrong or flagged. */
    fun newState(questionId: String, now: Instant, flagged: Boolean = false) = ReviewState(
        questionId = questionId,
        dueAt = now,
        flagged = flagged,
        updatedAt = now,
    )

    fun isMastered(state: ReviewState): Boolean =
        state.reps >= MASTERED_REPS && state.intervalDays >= MASTERED_INTERVAL_DAYS

    /** Whole days from today to the test date, at least 1; null without a date. */
    fun daysUntilTest(testDate: LocalDate?, now: Instant, zone: ZoneId): Int? = testDate?.let {
        ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), it).toInt().coerceAtLeast(1)
    }
}
