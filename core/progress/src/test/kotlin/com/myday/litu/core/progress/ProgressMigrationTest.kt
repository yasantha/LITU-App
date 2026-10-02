package com.myday.litu.core.progress

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.myday.litu.core.progress.db.ProgressDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every user.db version must migrate to the current one without losing data (spec section 16).
 * When adding version N, add a test that creates version N-1, inserts rows and checks them after
 * migrating; this test then covers the full chain from every earlier version.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ProgressDatabase::class.java)

    @Test fun everyVersionMigratesToLatest() {
        for (start in 1..ProgressDatabase.VERSION) {
            val name = "migration-$start.db"
            helper.createDatabase(name, start).apply {
                execSQL("INSERT INTO daily_stat (day, answered, correct, study_minutes, goal_met) VALUES ('2026-10-01', 5, 4, 3, 0)")
                close()
            }
            helper.runMigrationsAndValidate(name, ProgressDatabase.VERSION, true, *ProgressDatabase.MIGRATIONS).close()
            Room.databaseBuilder(ApplicationProvider.getApplicationContext(), ProgressDatabase::class.java, name)
                .addMigrations(*ProgressDatabase.MIGRATIONS)
                .allowMainThreadQueries()
                .build()
                .apply {
                    val row = query("SELECT answered FROM daily_stat WHERE day = '2026-10-01'", null)
                    check(row.moveToFirst() && row.getInt(0) == 5) { "progress lost migrating from v$start" }
                    row.close()
                    close()
                }
        }
    }
}
