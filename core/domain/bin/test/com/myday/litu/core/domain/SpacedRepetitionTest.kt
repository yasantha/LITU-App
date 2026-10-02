package com.myday.litu.core.domain

import com.myday.litu.core.domain.scheduling.SpacedRepetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SpacedRepetitionTest {
    private val now = Instant.parse("2026-10-01T10:00:00Z")
    private val fresh = SpacedRepetition.newState("Q-1", now)

    @Test fun wrongAnswerResetsAndComesBackTomorrow() {
        val s = SpacedRepetition.schedule(fresh.copy(reps = 4, intervalDays = 20, ease = 2.5), false, now, null)
        assertEquals(0, s.reps)
        assertEquals(1, s.intervalDays)
        assertEquals(1, s.lapses)
        assertEquals(2.3, s.ease, 1e-9)
        assertEquals(now.plus(1, ChronoUnit.DAYS), s.dueAt)
        assertEquals(now, s.updatedAt)
    }

    @Test fun easeNeverDropsBelowMinimum() {
        val s = SpacedRepetition.schedule(fresh.copy(ease = 1.35), false, now, null)
        assertEquals(1.3, s.ease, 1e-9)
    }

    @Test fun correctAnswersFollowOneThreeSevenThenEase() {
        var s = fresh
        val intervals = (1..5).map { s = SpacedRepetition.schedule(s, true, now, null); s.intervalDays }
        // 1, 3, 7, then ceil(7 × 2.65) = 19, then ceil(19 × 2.70) = 52
        assertEquals(listOf(1, 3, 7, 19, 52), intervals)
        assertEquals(5, s.reps)
        assertEquals(2.75, s.ease, 1e-9)
    }

    @Test fun easeIsCappedAtMaximum() {
        val s = SpacedRepetition.schedule(fresh.copy(ease = 2.79, reps = 3, intervalDays = 7), true, now, null)
        assertEquals(2.8, s.ease, 1e-9)
    }

    @Test fun intervalNeverPassesTheTestOrSixtyDays() {
        val long = fresh.copy(reps = 5, intervalDays = 50, ease = 2.8)
        assertEquals(60, SpacedRepetition.schedule(long, true, now, null).intervalDays)
        assertEquals(4, SpacedRepetition.schedule(long, true, now, 4).intervalDays)
    }

    @Test fun masteredNeedsThreeInARowAndAWeekInterval() {
        assertTrue(SpacedRepetition.isMastered(fresh.copy(reps = 3, intervalDays = 7)))
        assertFalse(SpacedRepetition.isMastered(fresh.copy(reps = 3, intervalDays = 4)))
        assertFalse(SpacedRepetition.isMastered(fresh.copy(reps = 2, intervalDays = 30)))
    }

    @Test fun daysUntilTestIsAtLeastOne() {
        assertNull(SpacedRepetition.daysUntilTest(null, now, ZoneOffset.UTC))
        assertEquals(23, SpacedRepetition.daysUntilTest(LocalDate.of(2026, 10, 24), now, ZoneOffset.UTC))
        assertEquals(1, SpacedRepetition.daysUntilTest(LocalDate.of(2026, 9, 1), now, ZoneOffset.UTC))
    }
}
