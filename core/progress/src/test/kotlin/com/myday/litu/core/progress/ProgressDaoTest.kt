package com.myday.litu.core.progress

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myday.litu.core.progress.db.ProgressDatabase
import com.myday.litu.core.progress.db.ReviewStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressDaoTest {
    private lateinit var db: ProgressDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ProgressDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() = db.close()

    @Test fun dailyStatAccumulatesAndKeepsGoalMet() = runTest {
        val dao = db.progressDao()
        dao.addToDailyStat("2026-10-01", 15, 10, 0, 20)
        assertEquals(0, dao.dailyStat("2026-10-01")!!.goalMet)
        dao.addToDailyStat("2026-10-01", 5, 5, 3, 20)
        val stat = dao.dailyStat("2026-10-01")!!
        assertEquals(20, stat.answered)
        assertEquals(15, stat.correct)
        assertEquals(1, stat.goalMet)
        // Raising the goal later does not un-count the day.
        dao.addToDailyStat("2026-10-01", 1, 0, 0, 40)
        assertEquals(1, dao.dailyStat("2026-10-01")!!.goalMet)
    }

    @Test fun dueReviewsAreOldestFirstAndCapped() = runTest {
        val dao = db.progressDao()
        listOf(300L, 100L, 200L, 900L).forEachIndexed { i, due ->
            dao.upsertReviewState(ReviewStateEntity("Q$i", 2.5, 1, due, 0, 0, 0, 0))
        }
        assertEquals(listOf("Q1", "Q2"), dao.dueReviews(now = 500, limit = 2).map { it.questionId })
        assertEquals(listOf("Q3"), dao.upcomingReviews(now = 500, limit = 5).map { it.questionId })
        assertEquals(4, dao.observeReviewStates().first().size)
    }
}
