package com.myday.litu

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.appcheck.FirebaseAppCheck
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.config.FirebaseAvailability
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.usecase.FinishMockUseCase
import com.myday.litu.core.sync.AccountRepository
import com.myday.litu.core.sync.BackupRepository
import com.myday.litu.reminder.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LituApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var account: AccountRepository
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var config: ConfigRepository
    @Inject lateinit var entitlements: EntitlementRepository
    @Inject lateinit var content: ContentRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var finishMock: FinishMockUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        installAppCheck()
        ReminderNotifications.createChannel(this)
        // Nothing here blocks the study flow: every step works offline and failures are logged.
        appScope.launch { runCatching { settings.setLastContentVersion(content.contentVersion()) } }
        appScope.launch { runCatching { finishMock.finishExpired() } }
        appScope.launch {
            runCatching {
                config.refresh()
                account.ensureSignedIn()
                entitlements.refresh()
                // One read on app start, only for users with a linked Google account (spec 12.3).
                backup.restore()
            }.onFailure { Log.w("LituApplication", "Startup sync skipped", it) }
        }
    }

    /** App Check with Play Integrity (debug provider in debug builds); enforce in the console after launch. */
    private fun installAppCheck() {
        if (!FirebaseAvailability.isConfigured(this)) return
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckFactory.get())
    }
}
