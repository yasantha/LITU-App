package com.myday.litu.core.config

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.myday.litu.core.domain.repository.AppConfig
import com.myday.litu.core.domain.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Remote Config keys from spec section 12.5. Defaults apply offline and before the first fetch. */
@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConfigRepository {

    private val state = MutableStateFlow(AppConfig())
    override val config: StateFlow<AppConfig> = state.asStateFlow()

    private val remote: FirebaseRemoteConfig? by lazy {
        if (!FirebaseAvailability.isConfigured(context)) return@lazy null
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS).build(),
            )
            setDefaultsAsync(DEFAULTS)
        }
    }

    init {
        remote?.let { state.value = read(it) }
    }

    override suspend fun refresh() {
        val rc = remote ?: return
        runCatching { rc.fetchAndActivate().await() }
            .onFailure { Log.w(TAG, "Remote Config fetch failed; keeping cached values", it) }
        state.value = read(rc)
    }

    private fun read(rc: FirebaseRemoteConfig) = AppConfig(
        minVersionCode = rc.getLong(MIN_VERSION_CODE),
        hiddenQuestionIds = parse<List<String>>(rc.getString(HIDDEN_QUESTION_IDS))?.toSet().orEmpty(),
        bannerMessage = rc.getString(BANNER_MESSAGE),
        mockChapterWeights = parse<Map<String, Double>>(rc.getString(MOCK_CHAPTER_WEIGHTS)).orEmpty(),
        freeMockCount = rc.getLong(FREE_MOCK_COUNT).toInt(),
        paywallVariant = rc.getString(PAYWALL_VARIANT).ifBlank { "a" },
    )

    private inline fun <reified T> parse(raw: String): T? =
        if (raw.isBlank()) null else runCatching { json.decodeFromString<T>(raw) }.getOrNull()

    private companion object {
        const val TAG = "RemoteConfig"
        const val FETCH_INTERVAL_SECONDS = 3600L
        const val MIN_VERSION_CODE = "min_version_code"
        const val HIDDEN_QUESTION_IDS = "hidden_question_ids"
        const val BANNER_MESSAGE = "banner_message"
        const val MOCK_CHAPTER_WEIGHTS = "mock_chapter_weights"
        const val FREE_MOCK_COUNT = "free_mock_count"
        const val PAYWALL_VARIANT = "paywall_variant"
        val DEFAULTS = mapOf<String, Any>(
            MIN_VERSION_CODE to 1L,
            HIDDEN_QUESTION_IDS to "[]",
            BANNER_MESSAGE to "",
            MOCK_CHAPTER_WEIGHTS to "",
            FREE_MOCK_COUNT to 1L,
            PAYWALL_VARIANT to "a",
        )
        val json = Json { ignoreUnknownKeys = true }
    }
}
