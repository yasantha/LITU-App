package com.myday.litu.core.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.myday.litu.core.content.db.ContentDatabase

/**
 * Content upgrade (spec section 8.3): on launch, compare the bundled content_version with the one
 * installed on the phone. If the bundle is newer, delete the installed content.db so Room copies the
 * new asset. user.db is a separate file and is never touched.
 */
internal object ContentInstaller {
    fun prepare(context: Context) {
        val bundled = bundledVersion(context) ?: return
        val dbFile = context.getDatabasePath(ContentDatabase.NAME)
        if (!dbFile.exists()) return
        val installed = installedVersion(dbFile.path)
        if (installed == null || bundled > installed) {
            context.deleteDatabase(ContentDatabase.NAME)
        }
    }

    fun bundledVersion(context: Context): Int? = runCatching {
        context.assets.open(ContentDatabase.VERSION_ASSET_PATH).bufferedReader().use { it.readText().trim().toInt() }
    }.getOrNull()

    private fun installedVersion(path: String): Int? = runCatching {
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT value FROM meta WHERE key = 'content_version'", null).use { c ->
                if (c.moveToFirst()) c.getString(0).toInt() else null
            }
        }
    }.getOrNull()
}
