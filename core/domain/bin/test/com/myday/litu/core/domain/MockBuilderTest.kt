package com.myday.litu.core.domain

import com.myday.litu.core.domain.mock.MockBuilder
import com.myday.litu.core.domain.mock.MockScoring
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionRef
import com.myday.litu.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class MockBuilderTest {
    private fun pool(vararg counts: Pair<String, Int>) = counts.flatMap { (ch, n) ->
        (1..n).map { QuestionRef("Q-$ch-$it", "$ch-S", ch) }
    }

    @Test fun allocatesProportionallyWithLargestRemainder() {
        // 24 × (10, 20, 30, 40)/100 = 2.4, 4.8, 7.2, 9.6 → 2, 5, 7, 10
        val slots = MockBuilder.allocate(mapOf("A" to 10, "B" to 20, "C" to 30, "D" to 40), emptyMap(), 24)
        assertEquals(mapOf("A" to 2, "B" to 5, "C" to 7, "D" to 10), slots)
    }

    @Test fun weightsOverrideProportions() {
        val slots = MockBuilder.allocate(mapOf("A" to 50, "B" to 50), mapOf("A" to 3.0, "B" to 1.0), 24)
        assertEquals(mapOf("A" to 18, "B" to 6), slots)
    }

    @Test fun neverAllocatesMoreThanAvailable() {
        val slots = MockBuilder.allocate(mapOf("A" to 2, "B" to 100), mapOf("A" to 1.0, "B" to 1.0), 24)
        assertEquals(mapOf("A" to 2, "B" to 22), slots)
    }

    @Test fun buildsTwentyFourUniqueQuestions() {
        val ids = MockBuilder(Random(1)).build(pool("CH2" to 10, "CH3" to 40, "CH4" to 30, "CH5" to 20))
        assertEquals(24, ids.size)
        assertEquals(24, ids.toSet().size)
    }

    @Test fun prefersQuestionsNotInRecentMocks() {
        val p = pool("CH3" to 48)
        val recent = p.take(24).map { it.id }.toSet()
        val ids = MockBuilder(Random(7)).build(p, recent)
        assertTrue(ids.none { it in recent })
    }

    @Test fun smallPoolReturnsEverything() {
        assertEquals(5, MockBuilder(Random(1)).build(pool("CH3" to 5)).size)
    }

    @Test fun scoringUsesExactSetMatchAndPassMark() {
        fun q(id: String, ch: String, type: QuestionType = QuestionType.SINGLE, correct: Set<String> = setOf("a")) =
            Question(id, "$ch-S", ch, type, "?", "", "", 1, null, listOf("a", "b", "c", "d").mapIndexed { i, o ->
                AnswerOption(o, o, o in correct, i)
            })
        val qs = (1..23).map { q("Q$it", if (it <= 10) "CH3" else "CH5") } +
            q("M", "CH5", QuestionType.MULTI, setOf("a", "b"))
        val answers = (1..17).associate { "Q$it" to listOf("a") } + ("M" to listOf("a"))
        val r = MockScoring.score(qs, answers)
        assertEquals(17, r.score)
        assertFalse(r.passed)
        assertEquals(1, r.shortBy)
        assertEquals(10, r.chapterBreakdown.getValue("CH3").correct)
        assertEquals(14, r.chapterBreakdown.getValue("CH5").total)
        assertTrue(MockScoring.score(qs, answers + ("M" to listOf("b", "a"))).passed)
    }

    @Test fun countdownUsesWallClockAndStopsAtZero() {
        val start = Instant.parse("2026-10-01T10:00:00Z")
        assertEquals(Duration.ofMinutes(45), MockScoring.remaining(start, start))
        assertEquals(Duration.ofMinutes(5), MockScoring.remaining(start, start.plusSeconds(40 * 60)))
        assertTrue(MockScoring.remaining(start, start.plusSeconds(46 * 60)).isZero)
    }
}
