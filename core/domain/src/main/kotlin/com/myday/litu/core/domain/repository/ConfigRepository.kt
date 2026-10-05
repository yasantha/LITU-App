package com.myday.litu.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Remote Config values (spec section 12.5), with in-app defaults when offline. */
data class AppConfig(
    val minVersionCode: Long = 1,
    val hiddenQuestionIds: Set<String> = emptySet(),
    val bannerMessage: String = "",
    val mockChapterWeights: Map<String, Double> = emptyMap(),
    val freeMockCount: Int = 1,
    val paywallVariant: String = "a",
    /** Turns AdMob ads for free users off without an app update. */
    val adsEnabled: Boolean = true,
)

interface ConfigRepository {
    val config: StateFlow<AppConfig>
    suspend fun refresh()
}
