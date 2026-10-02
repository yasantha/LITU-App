package com.myday.litu.core.model

/** A handbook chapter. Chapter 1 is study only ([inTest] = false). */
data class Chapter(
    val id: String,
    val number: Int,
    val title: String,
    val inTest: Boolean,
)

data class Section(
    val id: String,
    val chapterId: String,
    val title: String,
    val sort: Int,
)

enum class QuestionType(val dbValue: String) {
    /** 4 options, 1 correct. */
    SINGLE("single"),

    /** 4 options, exactly 2 correct ("choose 2"). */
    MULTI("multi"),

    /** 2 options, never shuffled. */
    TRUE_FALSE("truefalse");

    val requiredSelections: Int get() = if (this == MULTI) 2 else 1

    companion object {
        fun fromDb(value: String): QuestionType = entries.first { it.dbValue == value }
    }
}

data class AnswerOption(
    val id: String,
    val label: String,
    val isCorrect: Boolean,
    val sort: Int,
)

data class Question(
    val id: String,
    val sectionId: String,
    val chapterId: String,
    val type: QuestionType,
    val stem: String,
    val explanation: String,
    val handbookRef: String,
    val difficulty: Int,
    val audioKey: String?,
    val options: List<AnswerOption>,
) {
    val correctOptionIds: Set<String> get() = options.filter { it.isCorrect }.map { it.id }.toSet()

    /** A question is answered correctly only when the selected set equals the correct set. */
    fun isCorrect(selected: Collection<String>): Boolean = selected.toSet() == correctOptionIds
}

data class Note(
    val sectionId: String,
    val bodyMarkdown: String,
    val keyFacts: List<String>,
    val audioKey: String?,
)

/** Lightweight reference used for pools and lists, so the whole bank is never loaded. */
data class QuestionRef(
    val id: String,
    val sectionId: String,
    val chapterId: String,
)
