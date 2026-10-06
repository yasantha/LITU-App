package com.myday.litu.core.domain

import com.myday.litu.core.domain.repository.RatingStore
import com.myday.litu.core.domain.usecase.RatingMomentUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class RatingMomentTest {
    private val now = Instant.parse("2026-10-01T10:00:00Z")
    private val store = object : RatingStore {
        var last: Instant? = null
        override suspend fun lastAsked() = last
        override suspend fun markAsked(at: Instant) { last = at }
    }

    private fun at(i: Instant) = RatingMomentUseCase(store, Clock.fixed(i, ZoneOffset.UTC))

    @Test fun asksOnceThenWaitsAtLeast120Days() = runTest {
        assertTrue(at(now).shouldAsk())
        at(now).markAsked()
        assertFalse(at(now.plus(30, ChronoUnit.DAYS)).shouldAsk())
        assertTrue(at(now.plus(120, ChronoUnit.DAYS)).shouldAsk())
    }
}
