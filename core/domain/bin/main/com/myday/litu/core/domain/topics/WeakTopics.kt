package com.myday.litu.core.domain.topics

import com.myday.litu.core.model.SectionStat

object WeakTopics {
    const val MIN_ANSWERED = 3

    /** Sections with the lowest accuracy, among those with enough answers to judge. */
    fun weakest(stats: List<SectionStat>, limit: Int = 3): List<SectionStat> =
        stats.filter { it.answered >= MIN_ANSWERED && it.correct < it.answered }
            .sortedWith(compareBy<SectionStat> { it.correct.toDouble() / it.answered }.thenByDescending { it.answered })
            .take(limit)
}
