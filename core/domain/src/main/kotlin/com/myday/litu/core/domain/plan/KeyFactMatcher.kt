package com.myday.litu.core.domain.plan

import com.myday.litu.core.model.Question

/**
 * Finds the key fact in a section's notes that best explains a question, so "Read about this" can
 * show the fact the learner missed. Matching runs on the phone with simple word overlap, weighted
 * towards the correct answer and any numbers (dates matter in this test).
 */
object KeyFactMatcher {
    private val stopWords = setOf(
        "the", "and", "for", "was", "were", "which", "who", "what", "when", "where", "how", "with", "from",
        "this", "that", "these", "those", "its", "are", "did", "does", "his", "her", "their", "they", "she",
        "into", "has", "had", "have", "not", "but", "one", "two", "many", "most", "about", "after", "before",
        "true", "false", "statement", "following", "called", "known", "year",
    )

    internal fun tokens(text: String): Set<String> = Regex("[a-z0-9]+").findAll(text.lowercase())
        .map { it.value }
        .filter { (it.length > 2 || it.all(Char::isDigit)) && it !in stopWords }
        .map { if (it.length > 4 && it.endsWith('s')) it.dropLast(1) else it }
        .toSet()

    /** Index of the best matching fact, or null when nothing is a convincing match. */
    fun bestMatch(question: Question, facts: List<String>): Int? {
        val answer = tokens(question.options.filter { it.isCorrect }.joinToString(" ") { it.label })
        val context = tokens(question.stem + " " + question.explanation)
        val scored = facts.mapIndexed { i, fact ->
            val f = tokens(fact)
            val numbers = f.count { it.all(Char::isDigit) && (it in answer || it in context) }
            i to (3 * (answer intersect f).size + (context intersect f).size + 2 * numbers)
        }
        val (index, score) = scored.maxByOrNull { it.second } ?: return null
        return index.takeIf { score >= MIN_SCORE }
    }

    private const val MIN_SCORE = 3
}
