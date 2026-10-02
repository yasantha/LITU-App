package com.myday.litu.core.content

import com.myday.litu.core.content.db.ChapterEntity
import com.myday.litu.core.content.db.ContentDao
import com.myday.litu.core.content.db.NoteEntity
import com.myday.litu.core.content.db.QuestionWithOptions
import com.myday.litu.core.content.db.SectionEntity
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.model.AnswerOption
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.Note
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.QuestionRef
import com.myday.litu.core.model.QuestionType
import com.myday.litu.core.model.Section
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val dao: ContentDao,
    private val config: ConfigRepository,
) : ContentRepository {

    // content.db never changes while the app runs, so the small tables and the ID index are cached.
    private val mutex = Mutex()
    private var chaptersCache: List<Chapter>? = null
    private var sectionsCache: List<Section>? = null
    private var refsCache: List<QuestionRef>? = null

    private val hidden: Set<String> get() = config.config.value.hiddenQuestionIds

    override suspend fun contentVersion(): Int = dao.meta("content_version")?.toIntOrNull() ?: 0

    override suspend fun chapters(): List<Chapter> = mutex.withLock {
        chaptersCache ?: dao.chapters().map { it.toModel() }.also { chaptersCache = it }
    }

    override suspend fun chapter(id: String): Chapter? = chapters().firstOrNull { it.id == id }

    override suspend fun allSections(): List<Section> = mutex.withLock {
        sectionsCache ?: dao.sections().map { it.toModel() }.also { sectionsCache = it }
    }

    override suspend fun sections(chapterId: String): List<Section> =
        allSections().filter { it.chapterId == chapterId }.sortedBy { it.sort }

    override suspend fun section(id: String): Section? = allSections().firstOrNull { it.id == id }

    override suspend fun question(id: String): Question? = questions(listOf(id)).firstOrNull()

    override suspend fun questions(ids: List<String>): List<Question> {
        val wanted = ids.filter { it !in hidden }
        if (wanted.isEmpty()) return emptyList()
        val byId = wanted.chunked(SQLITE_VARIABLE_LIMIT).flatMap { dao.questions(it) }.associateBy { it.question.id }
        return wanted.mapNotNull { byId[it]?.toModel() }
    }

    override suspend fun questionRefs(): List<QuestionRef> {
        val all = mutex.withLock {
            refsCache ?: dao.activeRefs().map { QuestionRef(it.id, it.sectionId, it.chapterId) }.also { refsCache = it }
        }
        val h = hidden
        return if (h.isEmpty()) all else all.filter { it.id !in h }
    }

    override suspend fun questionRefsForChapter(chapterId: String) = questionRefs().filter { it.chapterId == chapterId }

    override suspend fun questionRefsForSection(sectionId: String) = questionRefs().filter { it.sectionId == sectionId }

    override suspend fun sampleQuestionIds(): List<String> {
        val raw = dao.meta("sample_question_ids") ?: return emptyList()
        val known = questionRefs().mapTo(HashSet()) { it.id }
        return json.decodeFromString<List<String>>(raw).filter { it in known }
    }

    override suspend fun note(sectionId: String): Note? = dao.note(sectionId)?.toModel()

    private fun ChapterEntity.toModel() = Chapter(id, number, title, inTest == 1)
    private fun SectionEntity.toModel() = Section(id, chapterId, title, sort)
    private fun NoteEntity.toModel() = Note(sectionId, bodyMd, json.decodeFromString(keyFacts), audioKey)

    private fun QuestionWithOptions.toModel() = Question(
        id = question.id,
        sectionId = question.sectionId,
        chapterId = chapterId,
        type = QuestionType.fromDb(question.type),
        stem = question.stem,
        explanation = question.explanation,
        handbookRef = question.handbookRef,
        difficulty = question.difficulty,
        audioKey = question.audioKey,
        options = options.sortedBy { it.sort }.map { AnswerOption(it.id, it.label, it.isCorrect == 1, it.sort) },
    )

    private companion object {
        const val SQLITE_VARIABLE_LIMIT = 900
        val json = Json { ignoreUnknownKeys = true }
    }
}
