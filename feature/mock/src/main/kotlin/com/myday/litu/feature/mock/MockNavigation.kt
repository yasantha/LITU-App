package com.myday.litu.feature.mock

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object MockTestsRoute

@Serializable data class LiveMockRoute(val mockId: String)

@Serializable data class MockResultsRoute(val mockId: String)

@Serializable data class AnswerReviewRoute(val mockId: String)

fun NavGraphBuilder.mockTestsScreen(onStart: (String) -> Unit, onResults: (String) -> Unit, onLocked: () -> Unit) {
    composable<MockTestsRoute> { MockTestsScreen(onStart = onStart, onResults = onResults, onLocked = onLocked) }
}

fun NavGraphBuilder.liveMockScreen(onFinished: (String) -> Unit, onLeave: () -> Unit) {
    composable<LiveMockRoute> { LiveMockScreen(onFinished = onFinished, onLeave = onLeave) }
}

fun NavGraphBuilder.mockResultsScreen(
    onDone: () -> Unit,
    onReviewAnswers: (String) -> Unit,
    onPractiseChapters: (List<String>) -> Unit,
) {
    composable<MockResultsRoute> { MockResultsScreen(onDone = onDone, onReviewAnswers = onReviewAnswers, onPractiseChapters = onPractiseChapters) }
}

fun NavGraphBuilder.answerReviewScreen(onBack: () -> Unit) {
    composable<AnswerReviewRoute> { AnswerReviewScreen(onBack = onBack) }
}
