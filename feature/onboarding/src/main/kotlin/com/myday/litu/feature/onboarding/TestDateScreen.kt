package com.myday.litu.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.goal.DailyGoal
import com.myday.litu.core.model.UserSettings
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** S03: test date and daily goal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TestDateScreen(onBack: () -> Unit, onContinue: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    ScreenColumn(
        topBar = { LituTopBar("", onNav = onBack) },
        bottomBar = {
            LituButton("Continue", { viewModel.saveDateAndGoal(); onContinue() }, Modifier.padding(16.dp), enabled = s.testDate != null || s.notBooked)
        },
    ) {
        Text("When is your test?", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        LituCard(onClick = { picking = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = c.primary)
                Text(
                    s.testDate?.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK)) ?: "Choose a date",
                    style = LituTheme.type.body, color = if (s.testDate != null) c.textPrimary else c.textSecondary,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(s.notBooked, role = Role.Checkbox, onValueChange = viewModel::setNotBooked),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(s.notBooked, null)
            Text("I haven't booked yet", style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.padding(start = 8.dp))
        }
        s.daysUntilTest?.let { days ->
            LituCard(color = c.primaryContainer, border = false) {
                Text(
                    "That's $days ${if (days == 1) "day" else "days"} – about ${DailyGoal.suggest(days)} questions a day gets you ready.",
                    style = LituTheme.type.body, color = c.textPrimary,
                )
            }
        }
        Text("Daily goal", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserSettings.DAILY_GOAL_OPTIONS.forEach { goal ->
                val selected = s.goal == goal
                Surface(
                    onClick = { viewModel.setGoal(goal) },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton; this.selected = selected },
                    shape = LituDimens.cardShape,
                    color = if (selected) c.primaryContainer else c.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.outline),
                ) {
                    Column(Modifier.padding(vertical = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$goal", style = LituTheme.type.headline, color = c.textPrimary)
                        Text("${DailyGoal.minutesFor(goal)} min", style = LituTheme.type.caption, color = c.textSecondary)
                    }
                }
            }
        }
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = s.testDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(), selectableDates = FutureDates)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                LituButton("OK", {
                    picker.selectedDateMillis?.let { viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }, fillWidth = false)
            },
            dismissButton = { LituButton("Cancel", { picking = false }, variant = ButtonVariant.TEXT, fillWidth = false) },
        ) { DatePicker(picker) }
    }
}

/** A test date cannot be in the past. */
@OptIn(ExperimentalMaterial3Api::class)
internal object FutureDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

    override fun isSelectableYear(year: Int): Boolean = year >= java.time.LocalDate.now().year
}
