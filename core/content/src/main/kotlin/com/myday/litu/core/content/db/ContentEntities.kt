package com.myday.litu.core.content.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// Tables mirror spec section 8.1. content/pipeline/build_db.py creates content.db from the schema
// Room exports for these entities, so the bundled file always passes Room's validation.

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "chapter")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val title: String,
    @ColumnInfo(name = "in_test", defaultValue = "1") val inTest: Int,
)

@Entity(
    tableName = "section",
    foreignKeys = [ForeignKey(entity = ChapterEntity::class, parentColumns = ["id"], childColumns = ["chapter_id"])],
    indices = [Index(value = ["chapter_id"], name = "idx_section_chapter")],
)
data class SectionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    val title: String,
    val sort: Int,
)

@Entity(
    tableName = "question",
    foreignKeys = [ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["section_id"])],
    indices = [Index(value = ["section_id", "active"], name = "idx_question_section")],
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    val type: String,
    val stem: String,
    val explanation: String,
    @ColumnInfo(name = "handbook_ref") val handbookRef: String,
    val difficulty: Int,
    @ColumnInfo(name = "audio_key") val audioKey: String?,
    @ColumnInfo(defaultValue = "1") val active: Int,
    @ColumnInfo(name = "added_in") val addedIn: Int,
)

@Entity(
    tableName = "answer_option",
    foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id"], childColumns = ["question_id"])],
    indices = [Index(value = ["question_id"], name = "idx_option_question")],
)
data class AnswerOptionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    val label: String,
    @ColumnInfo(name = "is_correct") val isCorrect: Int,
    val sort: Int,
)

@Entity(
    tableName = "note",
    foreignKeys = [ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["section_id"])],
)
data class NoteEntity(
    @PrimaryKey @ColumnInfo(name = "section_id") val sectionId: String,
    @ColumnInfo(name = "body_md") val bodyMd: String,
    @ColumnInfo(name = "key_facts") val keyFacts: String,
    @ColumnInfo(name = "audio_key") val audioKey: String?,
)

data class QuestionWithOptions(
    @Embedded val question: QuestionEntity,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    @Relation(parentColumn = "id", entityColumn = "question_id")
    val options: List<AnswerOptionEntity>,
)

data class QuestionRefRow(
    val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
)
