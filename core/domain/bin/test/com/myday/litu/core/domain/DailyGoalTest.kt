package com.myday.litu.core.domain

import com.myday.litu.core.domain.goal.DailyGoal
import com.myday.litu.core.domain.topics.WeakTopics
import com.myday.litu.core.model.SectionStat
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalTest {
    @Test fun suggestionSnapsToGoalOptions() {
        assertEquals(20, DailyGoal.suggest(23)) // spec example: 23 days → about 20 a day
        assertEquals(20, DailyGoal.suggest(22))
        assertEquals(40, DailyGoal.suggest(12))
        assertEquals(10, DailyGoal.suggest(90))
        assertEquals(40, DailyGoal.suggest(5))
        assertEquals(20, DailyGoal.suggest(null))
    }

    @Test fun minutesAndEstimates() {
        assertEquals(5, DailyGoal.minutesFor(10))
        assertEquals(4, DailyGoal.estimatedMinutes(8))
        assertEquals(0, DailyGoal.estimatedMinutes(0))
    }

    @Test fun weakTopicsNeedEnoughAnswers() {
        val stats = listOf(
            SectionStat("A", "CH3", 10, 3),
            SectionStat("B", "CH3", 2, 0),
            SectionStat("C", "CH5", 4, 2),
            SectionStat("D", "CH4", 5, 5),
        )
        assertEquals(listOf("A", "C"), WeakTopics.weakest(stats).map { it.sectionId })
    }
}
