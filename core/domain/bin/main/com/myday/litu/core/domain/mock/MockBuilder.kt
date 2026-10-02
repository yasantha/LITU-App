package com.myday.litu.core.domain.mock

import com.myday.litu.core.model.Exam
import com.myday.litu.core.model.QuestionRef
import kotlin.random.Random

/** Mock exam builder (spec section 9.3). */
class MockBuilder(private val random: Random = Random.Default) {

    /**
     * @param pool active questions in test chapters, hidden IDs already removed.
     * @param recentlyUsed questions used in the user's last 3 mocks; avoided where possible.
     * @param weights optional per-chapter override (Remote Config `mock_chapter_weights`).
     * @return question IDs in exam order.
     */
    fun build(
        pool: List<QuestionRef>,
        recentlyUsed: Set<String> = emptySet(),
        weights: Map<String, Double> = emptyMap(),
        size: Int = Exam.QUESTION_COUNT,
    ): List<String> {
        val byChapter = pool.distinctBy { it.id }.groupBy { it.chapterId }
        val available = byChapter.mapValues { it.value.size }
        val slots = allocate(available, weights, size)
        val picked = slots.flatMap { (chapterId, count) ->
            val (fresh, used) = byChapter.getValue(chapterId).partition { it.id !in recentlyUsed }
            (fresh.shuffled(random) + used.shuffled(random)).take(count)
        }
        return picked.map { it.id }.shuffled(random)
    }

    companion object {
        /**
         * Largest-remainder allocation of [size] slots, proportional to [weights] when given for a
         * chapter, otherwise to the chapter's share of the pool. Never allocates more than a chapter
         * has; spare slots go to chapters with capacity left.
         */
        fun allocate(available: Map<String, Int>, weights: Map<String, Double>, size: Int): Map<String, Int> {
            val chapters = available.filterValues { it > 0 }.keys.sorted()
            if (chapters.isEmpty()) return emptyMap()
            val target = minOf(size, chapters.sumOf { available.getValue(it) })
            val raw = chapters.associateWith { (weights[it] ?: available.getValue(it).toDouble()).coerceAtLeast(0.0) }
            val rawTotal = raw.values.sum().takeIf { it > 0.0 } ?: return evenAllocation(chapters, available, target)

            val exact = raw.mapValues { it.value / rawTotal * target }
            val result = exact.mapValues { (id, v) -> minOf(v.toInt(), available.getValue(id)) }.toMutableMap()
            var remaining = target - result.values.sum()
            val byRemainder = chapters.sortedWith(
                compareByDescending<String> { exact.getValue(it) - exact.getValue(it).toInt() }.thenBy { it },
            )
            while (remaining > 0) {
                val open = byRemainder.filter { result.getValue(it) < available.getValue(it) }
                if (open.isEmpty()) break
                for (id in open) {
                    if (remaining == 0) break
                    result[id] = result.getValue(id) + 1
                    remaining--
                }
            }
            return result.filterValues { it > 0 }
        }

        private fun evenAllocation(chapters: List<String>, available: Map<String, Int>, target: Int) =
            allocate(available, chapters.associateWith { 1.0 }, target)
    }
}
