package com.myday.litu.core.domain.streak

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class StreakInfo(
    val current: Int,
    val best: Int,
    /** Missed days kept by a freeze (shown as a snowflake chip). */
    val frozenDays: Set<LocalDate>,
    val countedToday: Boolean,
    val freezeAvailableThisWeek: Boolean,
) {
    companion object {
        val EMPTY = StreakInfo(0, 0, emptySet(), countedToday = false, freezeAvailableThisWeek = true)
    }
}

/**
 * Daily goal streak (spec section 9.4). A day counts when the goal was met. The streak is
 * consecutive counted days ending today or yesterday; one freeze per calendar week (Monday start)
 * keeps the streak through a single missed day.
 */
object Streaks {
    fun compute(countedDays: Set<LocalDate>, today: LocalDate): StreakInfo {
        val thisWeek = weekStart(today)
        val first = countedDays.filter { !it.isAfter(today) }.minOrNull()
            ?: return StreakInfo.EMPTY.copy(freezeAvailableThisWeek = true)

        var run = 0
        var best = 0
        val usedWeeks = mutableSetOf<LocalDate>()
        val frozen = mutableSetOf<LocalDate>()
        val runFrozen = mutableSetOf<LocalDate>()
        var day = first
        while (!day.isAfter(today)) {
            when {
                day in countedDays -> run++
                day == today -> Unit // Today is still in play.
                run > 0 && weekStart(day) !in usedWeeks &&
                    (day.plusDays(1) in countedDays || day.plusDays(1) == today) -> {
                    usedWeeks += weekStart(day)
                    runFrozen += day
                }
                else -> {
                    run = 0
                    runFrozen.clear()
                }
            }
            best = maxOf(best, run)
            day = day.plusDays(1)
        }
        frozen += runFrozen
        return StreakInfo(
            current = run,
            best = best,
            frozenDays = frozen,
            countedToday = today in countedDays,
            freezeAvailableThisWeek = thisWeek !in usedWeeks,
        )
    }

    private fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
