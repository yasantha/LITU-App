package com.myday.litu.core.domain

import com.myday.litu.core.domain.plan.KeyFactMatcher
import com.myday.litu.core.domain.plan.PlanStep
import com.myday.litu.core.domain.plan.StudyPlanUseCase
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.domain.usecase.RecordAnswerUseCase
import com.myday.litu.core.domain.usecase.ReviewUpdater
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionType
import com.myday.litu.core.model.StudyMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StudyPlanTest {
    private val day1 = Instant.parse("2026-10-01T09:00:00Z")

    @Test fun matcherFindsTheFactBehindTheQuestion() {
        val q = Question(
            "Q-CH3-EAR-003", "CH3-EAR", "CH3", QuestionType.SINGLE,
            "In which year did the Romans, under Emperor Claudius, invade Britain?",
            "The Romans returned under Emperor Claudius in AD 43 and ruled for about 400 years.", "", 3, null,
            listOf("AD 43", "55 BC", "AD 410", "AD 789").mapIndexed { i, l -> AnswerOption("o$i", l, i == 0, i) },
        )
        val facts = listOf(
            "AD 410: the Romans leave",
            "AD 43: Roman invasion under Emperor Claudius",
            "Sutton Hoo, Suffolk: Anglo-Saxon royal ship burial",
        )
        assertEquals(1, KeyFactMatcher.bestMatch(q, facts))
        assertNull(KeyFactMatcher.bestMatch(q, listOf("Haggis is a Scottish dish")))
    }

    private val questions = (1..10).map { question("H$it", chapter = "CH3") } + (1..10).map { question("G$it", chapter = "CH5") }
    private val content = FakeContent(questions)
    private val progress = FakeProgress()
    private val settings = FakeSettings()
    private val store = FakePlanStore()

    private fun planFor(at: Instant): StudyPlanUseCase {
        val clock = Clock.fixed(at, ZoneOffset.UTC)
        return StudyPlanUseCase(content, progress, settings, ProgressOverviewUseCase(content, progress, FakeConfig(), clock), store, clock)
    }

    private suspend fun answer(at: Instant, ids: List<String>, right: Boolean, mode: StudyMode = StudyMode.PRACTICE) {
        val clock = Clock.fixed(at, ZoneOffset.UTC)
        val record = RecordAnswerUseCase(progress, settings, ReviewUpdater(progress, settings, clock), clock)
        ids.forEach { id -> record(questions.first { it.id == id }, listOf(if (right) "$id-A" else "$id-B"), mode, "s", 1000) }
    }

    @Test fun noAnswersMeansNoPlan() = runTest {
        assertNull(planFor(day1)())
    }

    @Test fun oneWrongAnswerIsEnoughForAPlan() = runTest {
        answer(day1, listOf("G1"), right = false)
        answer(day1, listOf("H1"), right = true)
        val plan = planFor(day1)()!!
        assertFalse(plan.exploring)
        assertEquals("CH5-S", plan.steps.filterIsInstance<PlanStep.Read>().single().sectionId)
    }

    @Test fun tiesGoToTheChapterWithMostWrongAnswers() = runTest {
        // Two-section chapter CH5 with one wrong answer per section, versus one wrong in CH3.
        val qs = listOf(question("H1", "CH3"), question("G1", "CH5"), question("P1", "CH5").copy(sectionId = "CH5-P"))
        val c = FakeContent(qs)
        val clock = Clock.fixed(day1, ZoneOffset.UTC)
        val record = RecordAnswerUseCase(progress, settings, ReviewUpdater(progress, settings, clock), clock)
        qs.forEach { record(it, listOf("${it.id}-B"), StudyMode.PRACTICE, "s", 1000) }
        val plan = StudyPlanUseCase(c, progress, settings, ProgressOverviewUseCase(c, progress, FakeConfig(), clock), store, clock)()!!
        assertEquals(setOf("CH5-S", "CH5-P"), plan.steps.filterIsInstance<PlanStep.Practise>().single().sectionIds.toSet())
    }

    @Test fun allCorrectExploresTheLeastPractisedSection() = runTest {
        answer(day1, listOf("H1", "H2"), right = true)
        val plan = planFor(day1)()!!
        assertTrue(plan.exploring)
        assertEquals("CH5-S", plan.steps.filterIsInstance<PlanStep.Read>().single().sectionId)
    }

    @Test fun planFocusesOnTheWeakSectionAndTicksOffSteps() = runTest {
        answer(day1, listOf("H1", "H2", "H3", "H4"), right = false)
        answer(day1, listOf("G1", "G2", "G3", "G4"), right = true)
        val use = planFor(day1)
        val plan = use()!!
        val read = plan.steps.filterIsInstance<PlanStep.Read>().single()
        assertEquals("CH3-S", read.sectionId)
        assertFalse(read.done)
        assertTrue(plan.steps.none { it is PlanStep.Review }) // wrong answers are due tomorrow, not today

        use.markNoteRead("CH3-S")
        answer(day1, (1..8).map { "H$it" }, right = true)
        val after = use()!!
        assertTrue(after.complete)
        assertEquals(0, after.minutesLeft)
    }

    @Test fun nextDayShowsImprovementAndReviewStep() = runTest {
        answer(day1, listOf("H1", "H2", "H3", "H4"), right = false)
        planFor(day1)()
        val day2 = day1.plusSeconds(26 * 3600)
        answer(day2, (1..10).map { "H$it" } + (1..10).map { "H$it" }, right = true)
        // Rows from day 1 are due on day 2; recreate a due item for the review step.
        answer(day2, listOf("G1", "G2", "G3"), right = false)
        val day3 = day2.plusSeconds(26 * 3600)
        val plan = planFor(day3)()!!
        val improvement = plan.improvements.single()
        assertEquals(0, improvement.fromPercent)
        assertTrue(improvement.toPercent >= StudyPlanUseCase.GOOD_PERCENT)
        assertTrue(plan.steps.any { it is PlanStep.Review })
        assertEquals(LocalDate.of(2026, 10, 3), store.saved!!.day)
    }
}
