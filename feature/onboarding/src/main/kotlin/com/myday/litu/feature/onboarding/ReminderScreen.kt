package com.myday.litu.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import com.myday.litu.core.designsystem.theme.LituDimens
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituFilterChip
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.illustration.LineIllustration
import com.myday.litu.core.designsystem.theme.LituTheme
import java.time.LocalTime

private val QUICK_TIMES = listOf(LocalTime.of(8, 0), LocalTime.of(12, 30), LocalTime.of(19, 0))

/** S04: the notification permission is requested only after "Turn on reminders". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderScreen(onBack: () -> Unit, onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.finish(remindersOn = granted, onDone = onFinished)
    }
    ScreenColumn(
        topBar = { LituTopBar("", onNav = onBack) },
        bottomBar = {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Turn on reminders", {
                    if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else viewModel.finish(remindersOn = true, onDone = onFinished)
                })
                LituButton("Not now", { viewModel.finish(remindersOn = false, onDone = onFinished) }, variant = ButtonVariant.TEXT)
            }
        },
    ) {
        LineIllustration(Illustration.BUS_STOP, Modifier.align(Alignment.CenterHorizontally), size = 120.dp)
        Text("When should we remind you?", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        Text("One gentle reminder a day helps you keep your streak.", style = LituTheme.type.body, color = c.textSecondary)
        // A large tappable time opens the picker; an inline text field would pop the keyboard over the buttons.
        Surface(
            onClick = { picking = true },
            shape = LituDimens.cardShape,
            color = c.primaryContainer,
            modifier = Modifier.align(Alignment.CenterHorizontally).semantics { contentDescription = "Reminder time ${s.reminderTime}. Change time" },
        ) {
            Text(s.reminderTime.toString(), style = LituTheme.type.display, color = c.textPrimary, modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            QUICK_TIMES.forEach { t -> LituFilterChip(t.toString(), s.reminderTime == t, { viewModel.setTime(t) }) }
        }
    }
    if (picking) {
        val picker = rememberTimePickerState(s.reminderTime.hour, s.reminderTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = c.surface,
            title = { Text("Reminder time") },
            text = { TimePicker(picker) },
            confirmButton = { LituButton("OK", { viewModel.setTime(LocalTime.of(picker.hour, picker.minute)); picking = false }, fillWidth = false) },
            dismissButton = { LituButton("Cancel", { picking = false }, variant = ButtonVariant.TEXT, fillWidth = false) },
        )
    }
}
