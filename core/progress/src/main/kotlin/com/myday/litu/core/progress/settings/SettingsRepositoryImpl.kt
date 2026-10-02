package com.myday.litu.core.progress.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.model.ThemePreference
import com.myday.litu.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore keys from spec section 8.4. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = store.data.map { p ->
        UserSettings(
            testDate = p[TEST_DATE]?.let(LocalDate::parse),
            dailyGoal = p[DAILY_GOAL] ?: UserSettings.DEFAULT_DAILY_GOAL,
            reminderTime = p[REMINDER_TIME]?.let(LocalTime::parse) ?: UserSettings.DEFAULT_REMINDER_TIME,
            remindersEnabled = p[REMINDERS_ENABLED] ?: false,
            theme = ThemePreference.fromStorage(p[THEME]),
            audioAutoplay = p[AUDIO_AUTOPLAY] ?: false,
            audioSpeed = p[AUDIO_SPEED] ?: 1.0f,
            onboardingDone = p[ONBOARDING_DONE] ?: false,
            sampleDone = p[SAMPLE_DONE] ?: false,
            lastContentVersion = p[LAST_CONTENT_VERSION] ?: 0,
        )
    }

    override suspend fun current(): UserSettings = settings.first()

    override suspend fun setTestDate(date: LocalDate?) {
        store.edit { if (date == null) it.remove(TEST_DATE) else it[TEST_DATE] = date.toString() }
    }

    override suspend fun setDailyGoal(goal: Int) {
        store.edit { it[DAILY_GOAL] = goal }
    }

    override suspend fun setReminder(time: LocalTime, enabled: Boolean) {
        store.edit {
            it[REMINDER_TIME] = time.toString()
            it[REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[THEME] = theme.storageValue }
    }

    override suspend fun setAudioAutoplay(enabled: Boolean) {
        store.edit { it[AUDIO_AUTOPLAY] = enabled }
    }

    override suspend fun setAudioSpeed(speed: Float) {
        store.edit { it[AUDIO_SPEED] = speed.coerceIn(UserSettings.MIN_AUDIO_SPEED, UserSettings.MAX_AUDIO_SPEED) }
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[ONBOARDING_DONE] = done }
    }

    override suspend fun setSampleDone(done: Boolean) {
        store.edit { it[SAMPLE_DONE] = done }
    }

    override suspend fun setLastContentVersion(version: Int) {
        store.edit { it[LAST_CONTENT_VERSION] = version }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val TEST_DATE = stringPreferencesKey("test_date")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val THEME = stringPreferencesKey("theme")
        val AUDIO_AUTOPLAY = booleanPreferencesKey("audio_autoplay")
        val AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SAMPLE_DONE = booleanPreferencesKey("sample_done")
        val LAST_CONTENT_VERSION = intPreferencesKey("last_content_version")
    }
}
