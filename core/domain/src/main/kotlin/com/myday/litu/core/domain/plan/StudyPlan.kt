package com.myday.litu.core.domain.plan

import java.time.LocalDate

/** Today's improvement plan, fixed for the day so it does not reshuffle while the user works on it. */
data class SavedPlan(
    val day: LocalDate,
    val sectionIds: List<String>,
    /** Section accuracy (%) when the plan was made, to show progress later. */
    val baselinePercent: Map<String, Int>,
    val reviewTarget: Int,
    val notesRead: Set<String> = emptySet(),
    val improvements: List<Improvement> = emptyList(),
    /** No wrong answers yet, so the plan explores the least-practised section. */
    val exploring: Boolean = false,
)

data class Improvement(val sectionId: String, val fromPercent: Int, val toPercent: Int)

interface StudyPlanStore {
    suspend fun load(): SavedPlan?
    suspend fun save(plan: SavedPlan)
}

sealed interface PlanStep {
    val done: Boolean
    val minutes: Int

    data class Read(val sectionId: String, val title: String, val chapterNumber: Int, override val done: Boolean) : PlanStep {
        override val minutes get() = 3
    }

    data class Practise(val sectionIds: List<String>, val titles: List<String>, val questions: Int, override val done: Boolean) : PlanStep {
        override val minutes get() = (questions + 1) / 2
    }

    data class Review(val questions: Int, override val done: Boolean) : PlanStep {
        override val minutes get() = (questions + 1) / 2
    }
}

data class ImprovementView(val title: String, val fromPercent: Int, val toPercent: Int)

data class StudyPlan(
    val steps: List<PlanStep>,
    val improvements: List<ImprovementView>,
    /** True when the test is close and the plan focuses on the chapters with the most questions. */
    val testSoon: Boolean,
    /** True when there were no weak sections yet and the plan explores the least-practised one. */
    val exploring: Boolean,
) {
    val minutesLeft: Int get() = steps.filterNot { it.done }.sumOf { it.minutes }
    val nextStep: PlanStep? get() = steps.firstOrNull { !it.done }
    val complete: Boolean get() = steps.isNotEmpty() && steps.all { it.done }
}
