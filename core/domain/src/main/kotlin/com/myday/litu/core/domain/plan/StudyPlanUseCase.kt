package com.myday.litu.core.domain.plan

import com.myday.litu.core.domain.readiness.Readiness
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.scheduling.SpacedRepetition
import com.myday.litu.core.domain.usecase.ProgressOverview
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.model.Exam
import com.myday.litu.core.model.SectionStat
import com.myday.litu.core.model.StudyMode
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds a short daily plan for the user's weakest sections: read the note, practise questions on
 * those sections, then clear due reviews. A section is weak when it had a wrong answer in the last
 * 30 days and is under [GOOD_PERCENT] accuracy; more wrong answers rank higher. With the test a week
 * away or less, high-weight chapters come first.
 */
class StudyPlanUseCase @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val overviewUseCase: ProgressOverviewUseCase,
    private val store: StudyPlanStore,
    private val clock: Clock,
) {
    suspend operator fun invoke(): StudyPlan? {
        val today = LocalDate.now(clock)
        val overview = overviewUseCase()
        val daysLeft = SpacedRepetition.daysUntilTest(settings.current().testDate, clock.instant(), clock.zone)
        val testSoon = daysLeft != null && daysLeft <= TEST_SOON_DAYS
        val saved = store.load()?.takeIf { it.day == today } ?: newPlan(today, overview, testSoon) ?: return null

        val titles = overview.sections.associateBy { it.id }
        val chapterNumber = { sectionId: String ->
            titles[sectionId]?.let { s -> overview.chapter(s.chapterId)?.number } ?: 0
        }
        val todayStart = today.atStartOfDay(clock.zone).toInstant()
        val attemptsToday = progress.attemptsSince(todayStart)
        val refs = content.questionRefs().associateBy { it.id }
        val practisedToday = attemptsToday.count { a -> refs[a.questionId]?.sectionId in saved.sectionIds }
        val reviewedToday = attemptsToday.count { it.mode == StudyMode.REVIEW }

        val steps = buildList<PlanStep> {
            saved.sectionIds.filter { content.note(it) != null }.forEach { id ->
                add(PlanStep.Read(id, titles[id]?.title.orEmpty(), chapterNumber(id), done = id in saved.notesRead))
            }
            add(
                PlanStep.Practise(
                    sectionIds = saved.sectionIds,
                    titles = saved.sectionIds.mapNotNull { titles[it]?.title },
                    questions = PRACTICE_QUESTIONS,
                    done = practisedToday >= PRACTICE_QUESTIONS,
                ),
            )
            if (saved.reviewTarget > 0) {
                val dueNow = progress.dueReviews(clock.instant(), Exam.REVIEW_SESSION_CAP).count { it.questionId in refs }
                add(PlanStep.Review(saved.reviewTarget, done = reviewedToday >= saved.reviewTarget || dueNow == 0))
            }
        }
        return StudyPlan(
            steps = steps,
            improvements = saved.improvements.mapNotNull { i ->
                titles[i.sectionId]?.let { ImprovementView(it.title, i.fromPercent, i.toPercent) }
            },
            testSoon = testSoon,
            exploring = saved.exploring,
        )
    }

    /** Called when a note opens, so the matching Read step ticks off. */
    suspend fun markNoteRead(sectionId: String) {
        val saved = store.load() ?: return
        if (saved.day != LocalDate.now(clock) || sectionId !in saved.sectionIds || sectionId in saved.notesRead) return
        store.save(saved.copy(notesRead = saved.notesRead + sectionId))
    }

    private suspend fun newPlan(today: LocalDate, overview: ProgressOverview, testSoon: Boolean): SavedPlan? {
        val testChapters = overview.chapters.filter { it.inTest }.map { it.id }.toSet()
        val stats = overview.sectionStats.filter { it.chapterId in testChapters }
        if (stats.isEmpty()) return null
        val percent = { s: SectionStat -> s.correct * 100 / s.answered }
        val shares = Readiness.proportionalShares(
            content.questionRefs().filter { it.chapterId in testChapters }.groupingBy { it.chapterId }.eachCount(),
        )
        val wrong = { s: SectionStat -> s.answered - s.correct }
        // Ties go to the chapter with the most wrong answers, matching the results screens.
        val chapterWrong = stats.groupBy { it.chapterId }.mapValues { (_, list) -> list.sumOf(wrong) }
        val byWeakness = compareByDescending(wrong).thenByDescending<SectionStat> { chapterWrong[it.chapterId] ?: 0 }.thenBy(percent)
        // Even one wrong answer earns a section a place in the plan.
        val weak = stats.filter { wrong(it) > 0 && percent(it) < GOOD_PERCENT }
            .sortedWith(
                if (testSoon) compareByDescending<SectionStat> { shares[it.chapterId] ?: 0.0 }.then(byWeakness) else byWeakness,
            )
            .take(FOCUS_SECTIONS)
        // Nothing weak yet: explore the least-mastered test section instead.
        val focus = weak.map { it.sectionId }.ifEmpty {
            listOfNotNull(
                overview.sections.filter { it.chapterId in testChapters }
                    .minByOrNull { overview.sectionMastery[it.id]?.fraction ?: 0f }?.id,
            )
        }
        if (focus.isEmpty()) return null
        val current = stats.associate { it.sectionId to percent(it) }

        // Say when yesterday's focus improved, then move on.
        val previous = store.load()
        val improvements = previous?.sectionIds.orEmpty().mapNotNull { id ->
            val before = previous!!.baselinePercent[id] ?: return@mapNotNull null
            val now = current[id] ?: return@mapNotNull null
            Improvement(id, before, now).takeIf { now >= GOOD_PERCENT || now - before >= IMPROVED_BY }
        }
        val known = content.questionRefs().mapTo(HashSet()) { it.id }
        val due = progress.dueReviews(clock.instant(), Exam.REVIEW_SESSION_CAP).count { it.questionId in known }
        val plan = SavedPlan(
            day = today,
            sectionIds = focus,
            baselinePercent = focus.associateWith { current[it] ?: 0 },
            reviewTarget = minOf(due, REVIEW_QUESTIONS),
            improvements = improvements,
            exploring = weak.isEmpty(),
        )
        store.save(plan)
        return plan
    }

    companion object {
        const val GOOD_PERCENT = 80
        const val IMPROVED_BY = 15
        const val FOCUS_SECTIONS = 2
        const val PRACTICE_QUESTIONS = 8
        const val REVIEW_QUESTIONS = 10
        const val TEST_SOON_DAYS = 7
    }
}
