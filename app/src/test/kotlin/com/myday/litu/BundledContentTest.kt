package com.myday.litu

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myday.litu.core.content.ContentRepositoryImpl
import com.myday.litu.core.content.db.ContentDatabase
import com.myday.litu.core.domain.repository.AppConfig
import com.myday.litu.core.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The content.db built by content/pipeline/build_db.py must pass Room's prepackaged-database check
 * and match the spec's content rules. Fails if the pipeline and the Room schema drift apart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BundledContentTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.databaseBuilder(context, ContentDatabase::class.java, "content-test.db")
        .createFromAsset(ContentDatabase.ASSET_PATH)
        .allowMainThreadQueries()
        .build()
    private val hidden = MutableStateFlow(AppConfig())
    private val repo = ContentRepositoryImpl(db.contentDao(), object : ConfigRepository {
        override val config = hidden
        override suspend fun refresh() = Unit
    })

    @After fun tearDown() {
        db.close()
        context.deleteDatabase("content-test.db")
    }

    @Test fun bundledDatabaseOpensAndMatchesVersionMarker() = runTest {
        val marker = context.assets.open(ContentDatabase.VERSION_ASSET_PATH).bufferedReader().readText().trim().toInt()
        assertEquals(marker, repo.contentVersion())
        assertEquals(5, repo.chapters().size)
        assertTrue("chapter 1 is study only", repo.chapters().first { it.number == 1 }.inTest.not())
    }

    @Test fun questionsFollowTypeRules() = runTest {
        val refs = repo.questionRefs()
        assertTrue(refs.size >= 24)
        for (q in repo.questions(refs.map { it.id })) {
            val correct = q.options.count { it.isCorrect }
            when (q.type) {
                com.myday.litu.core.model.QuestionType.SINGLE -> assertEquals(q.id, 4 to 1, q.options.size to correct)
                com.myday.litu.core.model.QuestionType.MULTI -> assertEquals(q.id, 4 to 2, q.options.size to correct)
                com.myday.litu.core.model.QuestionType.TRUE_FALSE -> assertEquals(q.id, 2 to 1, q.options.size to correct)
            }
        }
        assertEquals(10, repo.sampleQuestionIds().size)
    }

    @Test fun hiddenQuestionsAreExcluded() = runTest {
        val first = repo.questionRefs().first().id
        hidden.value = AppConfig(hiddenQuestionIds = setOf(first))
        assertTrue(repo.questionRefs().none { it.id == first })
        assertEquals(null, repo.question(first))
    }
}
