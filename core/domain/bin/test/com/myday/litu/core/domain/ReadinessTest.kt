package com.myday.litu.core.domain

import com.myday.litu.core.domain.readiness.ChapterCoverage
import com.myday.litu.core.domain.readiness.Readiness
import com.myday.litu.core.domain.readiness.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadinessTest {
    @Test fun noMockMeansNoScore() {
        assertNull(Readiness.score(emptyList(), listOf(ChapterCoverage("CH2", 1.0, 10, 10))))
    }

    @Test fun mockPartUsesWeightsNewestFirst() {
        assertEquals(80.0, Readiness.mockPart(listOf(80.0))!!, 1e-9)
        // (90×0.5 + 60×0.3) / 0.8
        assertEquals(78.75, Readiness.mockPart(listOf(90.0, 60.0))!!, 1e-9)
        assertEquals(0.5 * 90 + 0.3 * 60 + 0.2 * 30, Readiness.mockPart(listOf(90.0, 60.0, 30.0, 0.0))!!, 1e-9)
    }

    @Test fun coverageIsShareWeightedMastery() {
        val chapters = listOf(
            ChapterCoverage("CH2", 0.25, 20, 20),
            ChapterCoverage("CH3", 0.75, 40, 10),
        )
        assertEquals((0.25 * 1.0 + 0.75 * 0.25) * 100, Readiness.coveragePart(chapters), 1e-9)
    }

    @Test fun scoreCombinesMocksAndCoverage() {
        val coverage = listOf(ChapterCoverage("CH3", 1.0, 100, 50))
        // round(0.65 × 75 + 0.35 × 50) = round(66.25)
        assertEquals(66, Readiness.score(listOf(75.0), coverage))
    }

    @Test fun levelsMatchTheSpecBands() {
        assertEquals(ReadinessLevel.KEEP_PRACTISING, ReadinessLevel.fromScore(59))
        assertEquals(ReadinessLevel.GETTING_CLOSE, ReadinessLevel.fromScore(60))
        assertEquals(ReadinessLevel.GETTING_CLOSE, ReadinessLevel.fromScore(79))
        assertEquals(ReadinessLevel.LIKELY_TO_PASS, ReadinessLevel.fromScore(80))
    }

    @Test fun proportionalSharesSumToOne() {
        val shares = Readiness.proportionalShares(mapOf("a" to 10, "b" to 30))
        assertEquals(0.25, shares.getValue("a"), 1e-9)
        assertEquals(0.75, shares.getValue("b"), 1e-9)
    }
}
