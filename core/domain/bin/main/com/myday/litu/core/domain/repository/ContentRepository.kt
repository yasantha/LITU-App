package com.myday.litu.core.domain.repository

import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.Note
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionRef
import com.myday.litu.core.model.Section

/**
 * Read-only access to content.db. Every query excludes inactive questions and the IDs hidden by
 * Remote Config, so callers never see a retired or faulty question.
 */
interface ContentRepository {
    suspend fun contentVersion(): Int
    suspend fun chapters(): List<Chapter>
    suspend fun chapter(id: String): Chapter?
    suspend fun sections(chapterId: String): List<Section>
    suspend fun allSections(): List<Section>
    suspend fun section(id: String): Section?

    /** Loads one question with its options in stored order (shuffling is a display concern). */
    suspend fun question(id: String): Question?

    /** Loads questions in the order of [ids]; unknown, inactive or hidden IDs are dropped. */
    suspend fun questions(ids: List<String>): List<Question>

    suspend fun questionRefs(): List<QuestionRef>
    suspend fun questionRefsForChapter(chapterId: String): List<QuestionRef>
    suspend fun questionRefsForSection(sectionId: String): List<QuestionRef>

    /** The free sample chosen by the content team (content.db meta key sample_question_ids). */
    suspend fun sampleQuestionIds(): List<String>

    suspend fun note(sectionId: String): Note?
}
