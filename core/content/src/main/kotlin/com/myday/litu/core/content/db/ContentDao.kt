package com.myday.litu.core.content.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ContentDao {
    @Query("SELECT value FROM meta WHERE key = :key")
    suspend fun meta(key: String): String?

    @Query("SELECT * FROM chapter ORDER BY number")
    suspend fun chapters(): List<ChapterEntity>

    @Query("SELECT * FROM section ORDER BY chapter_id, sort")
    suspend fun sections(): List<SectionEntity>

    @Transaction
    @Query(
        """SELECT q.*, s.chapter_id AS chapter_id FROM question q
           JOIN section s ON s.id = q.section_id
           WHERE q.id IN (:ids) AND q.active = 1""",
    )
    suspend fun questions(ids: List<String>): List<QuestionWithOptions>

    @Query(
        """SELECT q.id, q.section_id, s.chapter_id FROM question q
           JOIN section s ON s.id = q.section_id
           WHERE q.active = 1 ORDER BY q.id""",
    )
    suspend fun activeRefs(): List<QuestionRefRow>

    @Query("SELECT * FROM note WHERE section_id = :sectionId")
    suspend fun note(sectionId: String): NoteEntity?
}
