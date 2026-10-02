package com.myday.litu.core.domain.goal

import com.myday.litu.core.model.UserSettings
import kotlin.math.ceil

object DailyGoal {
    /** Rough number of answers that gets a learner through the bank with repeats. */
    const val TARGET_ANSWERS = 450

    /** Minutes shown on goal cards: 10 (5 min), 20 (10 min), 40 (20 min). */
    fun minutesFor(goal: Int): Int = goal / 2

    /** Suggested goal for the days left: the nearest goal option, ties going up (S03). */
    fun suggest(daysUntilTest: Int?): Int {
        if (daysUntilTest == null) return UserSettings.DEFAULT_DAILY_GOAL
        val perDay = TARGET_ANSWERS.toDouble() / daysUntilTest.coerceAtLeast(1)
        return UserSettings.DAILY_GOAL_OPTIONS.minWith(compareBy<Int> { kotlin.math.abs(it - perDay) }.thenByDescending { it })
    }

    /** Review queue estimate shown on S13: about 30 seconds per question. */
    fun estimatedMinutes(questionCount: Int): Int = if (questionCount == 0) 0 else ceil(questionCount / 2.0).toInt()
}
