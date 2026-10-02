package com.myday.litu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class StartDestination { WELCOME, SAMPLE, PAYWALL, HOME }

data class AppState(
    val loading: Boolean = true,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val start: StartDestination = StartDestination.WELCOME,
    val isPro: Boolean = false,
    val updateRequired: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    settings: SettingsRepository,
    private val entitlements: EntitlementRepository,
    config: ConfigRepository,
) : ViewModel() {
    val state: StateFlow<AppState> = combine(settings.settings, entitlements.isPro, config.config) { s, pro, cfg ->
        AppState(
            loading = false,
            theme = s.theme,
            // S01 routes to S02 on first launch, S06 when a trial has ended, otherwise S07.
            start = when {
                !s.onboardingDone -> StartDestination.WELCOME
                !s.sampleDone -> StartDestination.SAMPLE
                entitlements.lostPro -> StartDestination.PAYWALL
                else -> StartDestination.HOME
            },
            isPro = pro,
            updateRequired = BuildConfig.VERSION_CODE < cfg.minVersionCode,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())
}
