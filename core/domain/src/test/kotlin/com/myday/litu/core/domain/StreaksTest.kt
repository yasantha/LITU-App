package com.myday.litu.core.domain

import com.myday.litu.core.domain.streak.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreaksTest {
    // Thursday 1 October 2026
    private val today = LocalDate.of(2026, 10, 1)
    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test fun emptyHistory() {
        val s = Streaks.compute(emptySet(), today)
        assertEquals(0, s.current)
        assertTrue(s.freezeAvailableThisWeek)
    }

    @Test fun streakEndingTodayOrYesterday() {
        assertEquals(3, Streaks.compute(days(0, 1, 2), today).current)
        assertEquals(3, Streaks.compute(days(1, 2, 3), today).current)
        assertFalse(Streaks.compute(days(1, 2, 3), today).countedToday)
    }

    @Test fun freezeKeepsStreakThroughOneMissedDay() {
        // Missed Tuesday 29 Sept; Wednesday and today counted.
        val s = Streaks.compute(days(0, 1, 3, 4), today)
        assertEquals(4, s.current)
        assertEquals(setOf(today.minusDays(2)), s.frozenDays)
        assertFalse(s.freezeAvailableThisWeek)
    }

    @Test fun onlyOneFreezePerWeek() {
        // Missed Monday 28 Sept (freeze used) and Wednesday 30 Sept (same week, no freeze left).
        // Freezes are spent in date order, as they would have been on the day.
        val s = Streaks.compute(days(0, 2, 4), today)
        assertEquals(1, s.current)
        assertEquals(2, s.best)
    }

    @Test fun twoMissedDaysInARowBreakTheStreak() {
        assertEquals(1, Streaks.compute(days(0, 3, 4, 5), today).current)
    }

    @Test fun freezeCoversYesterdayWhileTodayIsStillOpen() {
        assertEquals(2, Streaks.compute(days(2, 3), today).current)
    }

    @Test fun bestStreakIsKept() {
        val s = Streaks.compute(days(0) + (10L..17L).map { today.minusDays(it) }, today)
        assertEquals(1, s.current)
        assertEquals(8, s.best)
    }
}
