package com.myday.litu.core.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.myday.litu.core.content.db.ContentDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Content upgrade (spec 8.3): a newer bundled content_version replaces the installed content.db,
 * and user.db is never touched. The test assets bundle content version 2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentInstallerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun installContent(version: Int) {
        val file = context.getDatabasePath(ContentDatabase.NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            db.execSQL("INSERT INTO meta VALUES ('content_version', '$version')")
        }
    }

    private fun installUserDb() {
        val file = context.getDatabasePath("user.db").apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { it.execSQL("CREATE TABLE progress (x INTEGER)") }
    }

    @Before fun clean() {
        context.deleteDatabase(ContentDatabase.NAME)
        context.deleteDatabase("user.db")
    }

    @Test fun olderInstalledContentIsReplacedAndProgressKept() {
        installContent(1)
        installUserDb()
        ContentInstaller.prepare(context)
        assertFalse(context.getDatabasePath(ContentDatabase.NAME).exists())
        assertTrue(context.getDatabasePath("user.db").exists())
    }

    @Test fun currentContentIsKept() {
        installContent(2)
        ContentInstaller.prepare(context)
        assertTrue(context.getDatabasePath(ContentDatabase.NAME).exists())
    }

    @Test fun firstInstallDoesNothing() {
        ContentInstaller.prepare(context)
        assertFalse(context.getDatabasePath(ContentDatabase.NAME).exists())
    }
}
