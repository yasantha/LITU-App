package com.myday.litu.core.model

import java.time.LocalDate
import java.time.LocalTime

enum class ThemePreference(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/** Mirrors the DataStore keys in spec section 8.4. */
data class UserSettings(
    val testDate: LocalDate? = null,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val remindersEnabled: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val audioAutoplay: Boolean = false,
    val audioSpeed: Float = 1.0f,
    val onboardingDone: Boolean = false,
    val sampleDone: Boolean = false,
    val lastContentVersion: Int = 0,
) {
    companion object {
        val DAILY_GOAL_OPTIONS = listOf(10, 20, 40)
        const val DEFAULT_DAILY_GOAL = 20
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(19, 0)
        const val MIN_AUDIO_SPEED = 0.75f
        const val MAX_AUDIO_SPEED = 1.5f
    }
}
