package com.myday.litu.core.content.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Read-only question bank bundled as assets/content/content.db. The Room version is the content
 * schema_version; content_version changes are handled by [ContentInstaller].
 */
@Database(
    entities = [
        MetaEntity::class,
        ChapterEntity::class,
        SectionEntity::class,
        QuestionEntity::class,
        AnswerOptionEntity::class,
        NoteEntity::class,
    ],
    version = ContentDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao

    companion object {
        const val SCHEMA_VERSION = 1
        const val NAME = "content.db"
        const val ASSET_PATH = "content/content.db"
        const val VERSION_ASSET_PATH = "content/content_version.txt"
    }
}
