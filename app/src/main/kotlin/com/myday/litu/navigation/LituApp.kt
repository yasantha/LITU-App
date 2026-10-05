package com.myday.litu.navigation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import com.myday.litu.core.ads.AdsEntryPoint
import com.myday.litu.core.ads.BannerAd
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myday.litu.AppState
import com.myday.litu.R
import com.myday.litu.StartDestination
import com.myday.litu.core.analytics.PaywallSource
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.icon.LogoMark
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.feature.home.HomeScreen
import com.myday.litu.feature.mock.AnswerReviewRoute
import com.myday.litu.feature.mock.LiveMockRoute
import com.myday.litu.feature.mock.MockResultsRoute
import com.myday.litu.feature.mock.MockTestsRoute
import com.myday.litu.feature.mock.answerReviewScreen
import com.myday.litu.feature.mock.liveMockScreen
import com.myday.litu.feature.mock.mockResultsScreen
import com.myday.litu.feature.mock.mockTestsScreen
import com.myday.litu.feature.notes.NotesRoute
import com.myday.litu.feature.notes.notesScreen
import com.myday.litu.feature.onboarding.ReminderRoute
import com.myday.litu.feature.onboarding.TestDateRoute
import com.myday.litu.feature.onboarding.WelcomeRoute
import com.myday.litu.feature.onboarding.onboardingScreens
import com.myday.litu.feature.paywall.PaywallRoute
import com.myday.litu.feature.paywall.paywallScreen
import com.myday.litu.feature.practice.ChapterDetailRoute
import com.myday.litu.feature.practice.PracticeHubRoute
import com.myday.litu.feature.practice.QuestionSessionRoute
import com.myday.litu.feature.practice.chapterDetailScreen
import com.myday.litu.feature.practice.practiceHubScreen
import com.myday.litu.feature.practice.questionSessionScreen
import com.myday.litu.feature.progress.ProgressRoute
import com.myday.litu.feature.progress.progressScreen
import com.myday.litu.feature.review.ReviewRoute
import com.myday.litu.feature.review.reviewScreen
import com.myday.litu.feature.settings.SettingsRoute
import com.myday.litu.feature.settings.settingsScreen
import com.myday.litu.feature.timer.TimerRoute
import com.myday.litu.feature.timer.timerScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object SplashRoute
@Serializable data object HomeRoute

private data class Tab(val route: Any, val label: String, val selected: ImageVector, val unselected: ImageVector, val lockable: Boolean = false)

private val tabs = listOf(
    Tab(HomeRoute, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Tab(PracticeHubRoute, "Practice", Icons.Rounded.School, Icons.Outlined.School, lockable = true),
    Tab(MockTestsRoute, "Mocks", Icons.Rounded.Timer, Icons.Outlined.Timer),
    Tab(ProgressRoute, "Progress", Icons.Rounded.ShowChart, Icons.Rounded.ShowChart),
)

/**
 * Four bottom tabs; Settings opens from the avatar on Home. First launch runs through onboarding,
 * the free sample, its results and the paywall (spec section 20).
 */
@Composable
fun LituApp(state: AppState) {
    if (state.updateRequired) return ForceUpdate()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val start = remember { state.start }
    val showBar = tabs.any { tab -> destination?.hasRoute(tab.route::class) == true }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ads = remember { EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java).adController() }
    // Ask for ad consent once the user reaches the main app, never during onboarding.
    val reachedHome = destination?.hasRoute(HomeRoute::class) == true
    LaunchedEffect(reachedHome) { if (reachedHome && !state.isPro) activity?.let(ads::requestConsent) }
    // Interstitials only at natural breaks: leaving a finished session or mock results.
    val homeAfterBreak = { ads.showInterstitialAtBreak(activity) { nav.goHome() } }

    Scaffold(
        containerColor = LituTheme.colors.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
            if (showBar) Column {
                BannerAd(ads, LituTheme.colors.surface)
                NavigationBar(containerColor = LituTheme.colors.surface) {
                    tabs.forEach { tab ->
                        val selected = destination?.hasRoute(tab.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.navigateToTab(tab.route) },
                            icon = {
                                BadgedBox(badge = {
                                    if (tab.lockable && !state.isPro) Badge(containerColor = LituTheme.colors.surfaceVariant) {
                                        Icon(Icons.Rounded.Lock, "Locked", Modifier.height(10.dp))
                                    }
                                }) { Icon(if (selected) tab.selected else tab.unselected, null) }
                            },
                            label = { Text(tab.label, style = LituTheme.type.caption) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LituTheme.colors.primary,
                                selectedTextColor = LituTheme.colors.primary,
                                indicatorColor = LituTheme.colors.primaryContainer,
                                unselectedIconColor = LituTheme.colors.textSecondary,
                                unselectedTextColor = LituTheme.colors.textSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            nav,
            startDestination = SplashRoute,
            modifier = Modifier.fillMaxSize().background(LituTheme.colors.background).padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable<SplashRoute> {
                SplashScreen {
                    nav.navigate(
                        when (start) {
                            StartDestination.WELCOME -> WelcomeRoute
                            StartDestination.SAMPLE -> QuestionSessionRoute.sample()
                            StartDestination.PAYWALL -> PaywallRoute(PaywallSource.LOCKED_ITEM.value)
                            StartDestination.HOME -> HomeRoute
                        },
                    ) { popUpTo<SplashRoute> { inclusive = true } }
                }
            }

            onboardingScreens(
                onWelcomeDone = { nav.navigate(TestDateRoute) },
                onTestDateDone = { nav.navigate(ReminderRoute) },
                onBack = { nav.popBackStack() },
                onFinished = { nav.navigate(QuestionSessionRoute.sample()) { popUpTo<WelcomeRoute> { inclusive = true } } },
            )

            paywallScreen(onClose = { nav.leavePaywall() }, onSubscribed = { nav.leavePaywall() })

            composable<HomeRoute> {
                HomeScreen(
                    onSettings = { nav.navigate(SettingsRoute) },
                    onMockTests = { nav.navigateToTab(MockTestsRoute) },
                    onTimer = { nav.navigate(TimerRoute) },
                    onReview = { nav.navigate(ReviewRoute) },
                    onContinue = { ids -> nav.navigate(if (ids.isEmpty()) QuestionSessionRoute.mixed() else QuestionSessionRoute.chapters(ids)) },
                    onPractiseSection = { nav.navigate(QuestionSessionRoute.section(it)) },
                    onLocked = { nav.openPaywall() },
                )
            }

            practiceHubScreen(
                onReview = { nav.navigate(ReviewRoute) },
                onStart = { nav.navigate(it) },
                onChapter = { nav.navigate(ChapterDetailRoute(it)) },
                onNotes = { nav.navigate(NotesRoute(it)) },
                onLocked = { nav.openPaywall() },
            )
            chapterDetailScreen(
                onBack = { nav.popBackStack() },
                onStart = { nav.navigate(it) },
                onNotes = { nav.navigate(NotesRoute(it)) },
            )
            questionSessionScreen(
                onClose = { if (!nav.popBackStack()) nav.goHome() },
                onDone = homeAfterBreak,
                onSampleFinished = {
                    nav.navigate(PaywallRoute(PaywallSource.ONBOARDING.value)) { popUpTo<QuestionSessionRoute> { inclusive = true } }
                },
                onPractiseAgain = { nav.navigate(it) { popUpTo<QuestionSessionRoute> { inclusive = true } } },
                onLocked = { nav.openPaywall { popUpTo<QuestionSessionRoute> { inclusive = true } } },
            )
            reviewScreen(
                onBack = { nav.popBackStack() },
                onStartReview = { nav.navigate(QuestionSessionRoute.review()) },
                onMixedPractice = { nav.navigate(QuestionSessionRoute.mixed()) { popUpTo<ReviewRoute> { inclusive = true } } },
            )
            notesScreen(onBack = { nav.popBackStack() }, onTestSection = { nav.navigate(QuestionSessionRoute.section(it)) })

            mockTestsScreen(
                onStart = { nav.navigate(LiveMockRoute(it)) },
                onResults = { nav.navigate(MockResultsRoute(it)) },
                onLocked = { nav.openPaywall() },
            )
            liveMockScreen(
                onFinished = { nav.navigate(MockResultsRoute(it)) { popUpTo<LiveMockRoute> { inclusive = true } } },
                onLeave = { nav.popBackStack() },
            )
            mockResultsScreen(
                onDone = homeAfterBreak,
                onReviewAnswers = { nav.navigate(AnswerReviewRoute(it)) },
                onPractiseChapters = { nav.navigate(QuestionSessionRoute.chapters(it)) },
            )
            answerReviewScreen(onBack = { nav.popBackStack() })

            progressScreen(onMockResults = { nav.navigate(MockResultsRoute(it)) })
            timerScreen(onBack = { nav.popBackStack() }, onQuiz = { nav.navigate(QuestionSessionRoute.timerQuiz()) })
            settingsScreen(
                onBack = { nav.popBackStack() },
                onPaywall = { nav.navigate(PaywallRoute(PaywallSource.SETTINGS.value)) },
                onDataDeleted = { nav.navigate(WelcomeRoute) { popUpTo(0) { inclusive = true } } },
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) = navigate(route) {
    popUpTo<HomeRoute> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private fun NavHostController.goHome() = navigate(HomeRoute) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

private fun NavHostController.openPaywall(builder: NavOptionsBuilder.() -> Unit = {}) =
    navigate(PaywallRoute(PaywallSource.LOCKED_ITEM.value), builder)

/** After the onboarding paywall (trial started or closed) go Home; otherwise return where the user was. */
private fun NavHostController.leavePaywall() {
    val fromOnboarding = previousBackStackEntry == null
    if (fromOnboarding || !popBackStack()) goHome()
}

private inline fun <reified T : Any> NavOptionsBuilder.popUpTo(noinline block: androidx.navigation.PopUpToBuilder.() -> Unit) =
    popUpTo(T::class as KClass<T>, block)

/** S01: logo on brand, app name, tagline and the four-colour strip. Under a second. */
@Composable
private fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(600)
        onDone()
    }
    val c = LituTheme.colors
    Box(Modifier.fillMaxSize().background(c.brand)) {
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoMark(200.dp)
            Text(stringResource(R.string.app_name), style = LituTheme.type.headline, color = c.onBrand, textAlign = TextAlign.Center)
            Text(stringResource(R.string.tagline), style = LituTheme.type.body, color = c.onBrandMuted, textAlign = TextAlign.Center)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(6.dp)) {
            listOf(c.brandAmber, c.brandCoral, c.plum, c.onBrand).forEach { Box(Modifier.weight(1f).fillMaxSize().background(it)) }
        }
    }
}

/** Remote Config min_version_code: below it, the app asks for an update before continuing. */
@Composable
private fun ForceUpdate() {
    val c = LituTheme.colors
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(c.background).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LogoMark(96.dp)
            Text("Please update the app", style = LituTheme.type.headline, color = c.textPrimary)
            Text("This version is out of date. Update to get the latest questions and fixes.", style = LituTheme.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
            LituButton("Update", {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()))
            })
        }
    }
}
