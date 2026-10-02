package com.myday.litu.core.progress.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        AttemptEntity::class,
        ReviewStateEntity::class,
        StudySessionEntity::class,
        MockExamEntity::class,
        DailyStatEntity::class,
        PendingReportEntity::class,
    ],
    version = ProgressDatabase.VERSION,
    exportSchema = true,
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        const val VERSION = 1
        const val NAME = "user.db"

        /**
         * Every user.db schema change adds an explicit migration here, with a test in
         * ProgressMigrationTest. Destructive migration is forbidden: this is the user's progress.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
