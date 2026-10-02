package com.myday.litu.core.domain.repository

import com.myday.litu.core.model.ThemePreference
import com.myday.litu.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings

    suspend fun setTestDate(date: LocalDate?)
    suspend fun setDailyGoal(goal: Int)
    suspend fun setReminder(time: LocalTime, enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setAudioAutoplay(enabled: Boolean)
    suspend fun setAudioSpeed(speed: Float)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setSampleDone(done: Boolean)
    suspend fun setLastContentVersion(version: Int)
    suspend fun clear()
}
