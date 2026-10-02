package com.myday.litu.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.litu.core.analytics.AnalyticsEvent
import com.myday.litu.core.analytics.AnalyticsLogger
import com.myday.litu.core.domain.goal.DailyGoal
import com.myday.litu.core.domain.repository.ReminderScheduler
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.scheduling.SpacedRepetition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class OnboardingState(
    val testDate: LocalDate? = null,
    val notBooked: Boolean = false,
    val goal: Int = 20,
    val daysUntilTest: Int? = null,
    val reminderTime: LocalTime = LocalTime.of(19, 0),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
    private val analytics: AnalyticsLogger,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settings.current()
            mutableState.value = OnboardingState(s.testDate, false, s.dailyGoal, days(s.testDate), s.reminderTime)
        }
    }

    private fun days(date: LocalDate?) = SpacedRepetition.daysUntilTest(date, clock.instant(), clock.zone)

    /** With a date, preselect the goal that gets the user ready in time. */
    fun setDate(date: LocalDate?) = mutableState.update {
        val days = days(date)
        it.copy(testDate = date, notBooked = date == null && it.notBooked, daysUntilTest = days, goal = if (date != null) DailyGoal.suggest(days) else it.goal)
    }

    fun setNotBooked(notBooked: Boolean) = mutableState.update {
        if (notBooked) it.copy(notBooked = true, testDate = null, daysUntilTest = null) else it.copy(notBooked = false)
    }

    fun setGoal(goal: Int) = mutableState.update { it.copy(goal = goal) }
    fun setTime(time: LocalTime) = mutableState.update { it.copy(reminderTime = time) }

    fun saveDateAndGoal() = viewModelScope.launch {
        val s = state.value
        settings.setTestDate(s.testDate)
        settings.setDailyGoal(s.goal)
    }

    fun finish(remindersOn: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        settings.setReminder(s.reminderTime, remindersOn)
        reminders.schedule(s.reminderTime, remindersOn)
        settings.setOnboardingDone(true)
        val saved = settings.current()
        analytics.log(AnalyticsEvent.OnboardingComplete(saved.testDate != null, saved.dailyGoal))
        onDone()
    }
}
