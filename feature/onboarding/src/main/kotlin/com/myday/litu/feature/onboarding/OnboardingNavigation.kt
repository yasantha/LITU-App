package com.myday.litu.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute
@Serializable data object TestDateRoute
@Serializable data object ReminderRoute

/** S02 → S03 → S04. The theme is not an onboarding step: System is right for almost everyone. */
fun NavGraphBuilder.onboardingScreens(
    onWelcomeDone: () -> Unit,
    onTestDateDone: () -> Unit,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    composable<WelcomeRoute> { WelcomeScreen(onContinue = onWelcomeDone) }
    composable<TestDateRoute> { TestDateScreen(onBack = onBack, onContinue = onTestDateDone) }
    composable<ReminderRoute> { ReminderScreen(onBack = onBack, onFinished = onFinished) }
}
