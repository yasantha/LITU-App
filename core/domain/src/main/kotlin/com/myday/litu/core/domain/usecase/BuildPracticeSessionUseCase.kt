package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.readiness.Readiness
import com.myday.litu.core.model.Exam
import com.myday.litu.core.model.QuestionRef
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.random.Random

sealed interface PracticeRequest {
    data object Sample : PracticeRequest
    data object Mixed : PracticeRequest
    data object Review : PracticeRequest
    data object TimerQuiz : PracticeRequest
    data class Chapter(val chapterId: String) : PracticeRequest
    data class Section(val sectionId: String) : PracticeRequest
    data class Chapters(val chapterIds: List<String>) : PracticeRequest
}

/** Picks the questions for a practice session. Returns question IDs in session order. */
class BuildPracticeSessionUseCase @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val clock: Clock,
) {
    var random: Random = Random.Default

    suspend operator fun invoke(request: PracticeRequest): List<String> = when (request) {
        PracticeRequest.Sample -> content.sampleQuestionIds().take(Exam.SAMPLE_QUESTION_COUNT)
        PracticeRequest.Mixed -> content.questionRefs().shuffled(random).take(Exam.MIXED_PRACTICE_COUNT).map { it.id }
        PracticeRequest.Review -> reviewQueue(Exam.REVIEW_SESSION_CAP)
        PracticeRequest.TimerQuiz -> {
            val due = reviewQueue(Exam.TIMER_QUIZ_COUNT)
            val extra = content.questionRefs().filter { it.id !in due }.shuffled(random)
            (due + extra.map { it.id }).take(Exam.TIMER_QUIZ_COUNT)
        }
        is PracticeRequest.Chapter -> prioritise(content.questionRefsForChapter(request.chapterId))
        is PracticeRequest.Section -> prioritise(content.questionRefsForSection(request.sectionId))
        is PracticeRequest.Chapters -> prioritise(request.chapterIds.flatMap { content.questionRefsForChapter(it) })
    }

    /** Due review items, oldest first, capped; orphaned IDs (not in content.db) are ignored. */
    suspend fun reviewQueue(limit: Int): List<String> {
        val known = content.questionRefs().mapTo(HashSet()) { it.id }
        return progress.dueReviews(clock.instant(), Int.MAX_VALUE)
            .filter { it.questionId in known }
            .take(limit)
            .map { it.questionId }
    }

    /** Questions not yet answered correctly in the last 30 days come first. */
    private suspend fun prioritise(refs: List<QuestionRef>): List<String> {
        val since = clock.instant().minus(Readiness.COVERAGE_WINDOW_DAYS, ChronoUnit.DAYS)
        val known = progress.attemptsSince(since).filter { it.correct }.mapTo(HashSet()) { it.questionId }
        val (fresh, seen) = refs.distinctBy { it.id }.partition { it.id !in known }
        return (fresh.shuffled(random) + seen.shuffled(random)).take(Exam.MIXED_PRACTICE_COUNT).map { it.id }
    }
}
