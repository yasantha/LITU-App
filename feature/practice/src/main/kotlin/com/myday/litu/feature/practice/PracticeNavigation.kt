package com.myday.litu.feature.practice

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myday.litu.core.domain.usecase.PracticeRequest
import kotlinx.serialization.Serializable

@Serializable data object PracticeHubRoute

@Serializable data class ChapterDetailRoute(val chapterId: String)

/** One practice session (S11 and its summary S12, or the free sample S05 and S05b). */
@Serializable data class QuestionSessionRoute(val kind: String, val arg: String? = null) {
    fun toRequest(): PracticeRequest = when (kind) {
        SAMPLE -> PracticeRequest.Sample
        MIXED -> PracticeRequest.Mixed
        REVIEW -> PracticeRequest.Review
        TIMER -> PracticeRequest.TimerQuiz
        CHAPTER -> PracticeRequest.Chapter(arg!!)
        SECTION -> PracticeRequest.Section(arg!!)
        SECTIONS -> PracticeRequest.Sections(arg!!.split(','), PLAN_QUESTIONS)
        else -> PracticeRequest.Chapters(arg!!.split(','))
    }

    companion object {
        const val SAMPLE = "sample"
        const val MIXED = "mixed"
        const val REVIEW = "review"
        const val TIMER = "timer"
        const val CHAPTER = "chapter"
        const val SECTION = "section"
        const val CHAPTERS = "chapters"
        const val SECTIONS = "sections"
        private const val PLAN_QUESTIONS = 8

        fun sample() = QuestionSessionRoute(SAMPLE)
        fun mixed() = QuestionSessionRoute(MIXED)
        fun review() = QuestionSessionRoute(REVIEW)
        fun timerQuiz() = QuestionSessionRoute(TIMER)
        fun chapter(id: String) = QuestionSessionRoute(CHAPTER, id)
        fun section(id: String) = QuestionSessionRoute(SECTION, id)
        fun chapters(ids: List<String>) = QuestionSessionRoute(CHAPTERS, ids.joinToString(","))
        fun sections(ids: List<String>) = QuestionSessionRoute(SECTIONS, ids.joinToString(","))
    }
}

fun NavGraphBuilder.practiceHubScreen(
    onReview: () -> Unit,
    onStart: (QuestionSessionRoute) -> Unit,
    onChapter: (String) -> Unit,
    onNotes: (sectionId: String) -> Unit,
    onLocked: () -> Unit,
) {
    composable<PracticeHubRoute> {
        PracticeHubScreen(onReview = onReview, onStart = onStart, onChapter = onChapter, onNotes = onNotes, onLocked = onLocked)
    }
}

fun NavGraphBuilder.chapterDetailScreen(
    onBack: () -> Unit,
    onStart: (QuestionSessionRoute) -> Unit,
    onNotes: (sectionId: String) -> Unit,
) {
    composable<ChapterDetailRoute> { ChapterDetailScreen(onBack = onBack, onStart = onStart, onNotes = onNotes) }
}

/**
 * @param onSampleFinished after the sample results (S05b): "See plans" opens the paywall.
 * @param onDone summaries return Home.
 */
fun NavGraphBuilder.questionSessionScreen(
    onClose: () -> Unit,
    onDone: () -> Unit,
    onSampleFinished: () -> Unit,
    onPractiseAgain: (QuestionSessionRoute) -> Unit,
    onLocked: () -> Unit,
    onReadAbout: (sectionId: String, questionId: String) -> Unit,
) {
    composable<QuestionSessionRoute> {
        QuestionSessionScreen(
            onReadAbout = onReadAbout,
            onClose = onClose,
            onDone = onDone,
            onSampleFinished = onSampleFinished,
            onPractiseAgain = onPractiseAgain,
            onLocked = onLocked,
        )
    }
}
