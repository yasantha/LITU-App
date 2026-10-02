package com.myday.litu.feature.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.billing.PurchaseOutcome
import com.myday.litu.core.domain.repository.AppInfo
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.ReminderScheduler
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.model.ThemePreference
import com.myday.litu.core.model.UserSettings
import com.myday.litu.core.sync.AccountRepository
import com.myday.litu.core.sync.AccountResult
import com.myday.litu.core.sync.AccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class SettingsState(
    val settings: UserSettings = UserSettings(),
    val account: AccountState = AccountState.SignedOut,
    val isPro: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val contentVersion: Int = 0,
    val versionName: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val account: AccountRepository,
    private val entitlements: EntitlementRepository,
    private val progress: ProgressRepository,
    private val content: ContentRepository,
    private val reminders: ReminderScheduler,
    private val appInfo: AppInfo,
) : ViewModel() {
    private val ui = MutableStateFlow(SettingsState(versionName = appInfo.versionName))

    val state: StateFlow<SettingsState> = combine(ui, settings.settings, account.state, entitlements.isPro) { u, s, a, pro ->
        u.copy(settings = s, account = a, isPro = pro)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ui.value)

    init {
        viewModelScope.launch { ui.value = ui.value.copy(contentVersion = content.contentVersion()) }
    }

    private fun message(text: String?) {
        ui.value = ui.value.copy(message = text, busy = false)
    }

    fun clearMessage() = message(null)

    fun setTheme(theme: ThemePreference) = viewModelScope.launch { settings.setTheme(theme) }
    fun setTestDate(date: LocalDate?) = viewModelScope.launch { settings.setTestDate(date) }
    fun setGoal(goal: Int) = viewModelScope.launch { settings.setDailyGoal(goal) }
    fun setAutoplay(on: Boolean) = viewModelScope.launch { settings.setAudioAutoplay(on) }
    fun setSpeed(speed: Float) = viewModelScope.launch { settings.setAudioSpeed(speed) }

    fun setReminder(time: LocalTime, enabled: Boolean) = viewModelScope.launch {
        settings.setReminder(time, enabled)
        reminders.schedule(time, enabled)
    }

    fun linkGoogle(activity: Activity?) {
        activity ?: return
        viewModelScope.launch {
            ui.value = ui.value.copy(busy = true)
            message(
                when (val r = account.linkGoogle(activity)) {
                    AccountResult.Success -> "Your progress is now backed up."
                    AccountResult.Restored -> "We found your backup and restored your progress."
                    AccountResult.Cancelled -> null
                    is AccountResult.Failed -> r.message
                },
            )
        }
    }

    fun signOut() = viewModelScope.launch {
        account.signOut()
        message("Signed out. Your progress stays on this phone.")
    }

    fun restore() = viewModelScope.launch {
        ui.value = ui.value.copy(busy = true)
        message(
            when (val r = entitlements.restore()) {
                is PurchaseOutcome.Success -> "Your subscription has been restored."
                is PurchaseOutcome.Failed -> r.message
                PurchaseOutcome.Cancelled -> null
            },
        )
    }

    /** Removes user.db, the Firestore backup and the Auth user (spec S20), then settings. */
    fun deleteData(onDeleted: () -> Unit) = viewModelScope.launch {
        ui.value = ui.value.copy(busy = true)
        when (val r = account.deleteAccount()) {
            is AccountResult.Failed -> message(r.message)
            else -> {
                progress.deleteAll()
                reminders.schedule(UserSettings.DEFAULT_REMINDER_TIME, enabled = false)
                settings.clear()
                message(null)
                onDeleted()
            }
        }
    }
}
