package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.readiness.ChapterCoverage
import com.myday.litu.core.domain.readiness.Readiness
import com.myday.litu.core.domain.readiness.ReadinessLevel
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.topics.WeakTopics
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.QuestionRef
import com.myday.litu.core.model.Section
import com.myday.litu.core.model.SectionStat
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class Mastery(val correct: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else (correct.toFloat() / total).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).toInt()
}

data class ReadinessPoint(val at: Instant, val score: Int)

data class ProgressOverview(
    val chapters: List<Chapter>,
    val sections: List<Section>,
    val chapterMastery: Map<String, Mastery>,
    val sectionMastery: Map<String, Mastery>,
    val readiness: Int?,
    val readinessLevel: ReadinessLevel?,
    val readinessHistory: List<ReadinessPoint>,
    val recentMocks: List<MockExam>,
    val weakestSections: List<SectionStat>,
) {
    fun section(id: String) = sections.firstOrNull { it.id == id }
    fun chapter(id: String) = chapters.firstOrNull { it.id == id }

    /** Test chapters ordered weakest first. */
    fun weakestChapters(limit: Int): List<Chapter> =
        chapters.filter { it.inTest }.sortedBy { chapterMastery[it.id]?.fraction ?: 0f }.take(limit)
}

/**
 * Mastery, readiness and weakest topics in one pass over the last 30 days of attempts.
 * chapterMastery = questions answered correctly in the last 30 days ÷ active questions (spec 9.2).
 */
class ProgressOverviewUseCase @Inject constructor(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val config: ConfigRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): ProgressOverview {
        val now = clock.instant()
        val chapters = content.chapters()
        val sections = content.allSections()
        val refs = content.questionRefs()
        val mocks = progress.recentFinishedMocks(HISTORY_MOCKS)
        val windowStart = (mocks.lastOrNull()?.finishedAt ?: now).minus(Readiness.COVERAGE_WINDOW_DAYS, ChronoUnit.DAYS)
        val attempts = progress.attemptsSince(minOf(windowStart, now.minus(Readiness.COVERAGE_WINDOW_DAYS, ChronoUnit.DAYS)))
        val refById = refs.associateBy { it.id }

        val recent = attempts.filter { it.answeredAt >= now.minus(Readiness.COVERAGE_WINDOW_DAYS, ChronoUnit.DAYS) }
        val correctIds = recent.filter { it.correct }.mapTo(HashSet()) { it.questionId }
        val chapterMastery = mastery(refs, correctIds) { it.chapterId }
        val sectionMastery = mastery(refs, correctIds) { it.sectionId }

        val history = mocks.indices.reversed().mapNotNull { i ->
            val upTo = mocks.drop(i)
            val at = upTo.first().finishedAt ?: return@mapNotNull null
            readinessAt(at, upTo, attempts, refs, chapters)?.let { ReadinessPoint(at, it) }
        }
        val readiness = readinessAt(now, mocks, attempts, refs, chapters)

        val sectionStats = recent.mapNotNull { a -> refById[a.questionId]?.let { it to a } }
            .groupBy { it.first.sectionId }
            .map { (sectionId, pairs) ->
                SectionStat(sectionId, pairs.first().first.chapterId, pairs.size, pairs.count { it.second.correct })
            }
        return ProgressOverview(
            chapters = chapters,
            sections = sections,
            chapterMastery = chapterMastery,
            sectionMastery = sectionMastery,
            readiness = readiness,
            readinessLevel = readiness?.let(ReadinessLevel::fromScore),
            readinessHistory = history,
            recentMocks = mocks,
            weakestSections = WeakTopics.weakest(sectionStats),
        )
    }

    private fun readinessAt(
        at: Instant,
        mocksNewestFirst: List<MockExam>,
        attempts: List<Attempt>,
        refs: List<QuestionRef>,
        chapters: List<Chapter>,
    ): Int? {
        val scores = mocksNewestFirst.filter { (it.finishedAt ?: Instant.MAX) <= at }
            .mapNotNull { m -> m.score?.let { it * 100.0 / m.questionIds.size.coerceAtLeast(1) } }
        val from = at.minus(Readiness.COVERAGE_WINDOW_DAYS, ChronoUnit.DAYS)
        val correct = attempts.filter { it.correct && it.answeredAt in from..at }.mapTo(HashSet()) { it.questionId }
        val testChapters = chapters.filter { it.inTest }.map { it.id }.toSet()
        val active = refs.filter { it.chapterId in testChapters }.groupingBy { it.chapterId }.eachCount()
        val weights = config.config.value.mockChapterWeights.filterKeys { it in testChapters }
        val shares = if (weights.isNotEmpty()) weights else Readiness.proportionalShares(active)
        val coverage = active.map { (chapterId, count) ->
            ChapterCoverage(
                chapterId = chapterId,
                share = shares[chapterId] ?: 0.0,
                activeQuestions = count,
                correctRecently = refs.count { it.chapterId == chapterId && it.id in correct },
            )
        }
        return Readiness.score(scores, coverage)
    }

    private fun mastery(refs: List<QuestionRef>, correct: Set<String>, key: (QuestionRef) -> String) =
        refs.groupBy(key).mapValues { (_, qs) -> Mastery(qs.count { it.id in correct }, qs.size) }

    companion object {
        const val HISTORY_MOCKS = 10
    }
}
