package com.myday.litu.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.InfoBanner
import com.myday.litu.core.designsystem.component.LegalLinks
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.ThemePicker
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.goal.DailyGoal
import com.myday.litu.core.model.UserSettings
import com.myday.litu.core.sync.AccountState
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable data object SettingsRoute

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit, onPaywall: () -> Unit, onDataDeleted: () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(onBack, onPaywall, onDataDeleted) }
}

private enum class Dialog { NONE, DATE, GOAL, TIME, DELETE }

/** S20. Theme defaults to System and applies instantly (19.7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(onBack: () -> Unit, onPaywall: () -> Unit, onDataDeleted: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var dialog by rememberSaveable { mutableStateOf(Dialog.NONE) }
    val settings = s.settings
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setReminder(settings.reminderTime, granted)
    }

    ScreenColumn(topBar = { LituTopBar("Settings", onNav = onBack) }, spacing = 8.dp) {
        s.message?.let { InfoBanner(it, icon = Icons.Rounded.Info) }

        Group("Backup") {
            when (val a = s.account) {
                is AccountState.Linked -> {
                    Item(Icons.Rounded.CloudUpload, "Backed up", a.email ?: "Google account linked")
                    Item(Icons.AutoMirrored.Rounded.Logout, "Sign out", onClick = viewModel::signOut)
                }
                AccountState.Unavailable -> Item(Icons.Rounded.CloudUpload, "Back up my progress", "Not available in this build")
                else -> Item(Icons.Rounded.CloudUpload, "Back up my progress", "Link a Google account · not backed up yet", onClick = { viewModel.linkGoogle(activity) })
            }
            Item(Icons.Rounded.DeleteForever, "Delete my data", "Removes your progress from this phone and online", onClick = { dialog = Dialog.DELETE })
        }

        Group("Study") {
            Item(Icons.Rounded.CalendarMonth, "Test date", settings.testDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)) ?: "Not booked", onClick = { dialog = Dialog.DATE })
            Item(Icons.Rounded.Flag, "Daily goal", "${settings.dailyGoal} questions", onClick = { dialog = Dialog.GOAL })
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = c.textPrimary)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text("Daily reminder", style = LituTheme.type.body, color = c.textPrimary)
                    Text(settings.reminderTime.toString(), style = LituTheme.type.caption, color = c.textSecondary,
                        modifier = Modifier.selectable(false, role = Role.Button) { dialog = Dialog.TIME })
                }
                Switch(settings.remindersEnabled, { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else viewModel.setReminder(settings.reminderTime, on)
                })
            }
        }

        Group("Display") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Rounded.Contrast, null, tint = c.textPrimary)
                    Text("Theme", style = LituTheme.type.body, color = c.textPrimary)
                }
                ThemePicker(settings.theme, viewModel::setTheme)
            }
            Item(Icons.Rounded.TextFields, "Text size", "Follows your phone's font size. Preview: The pass mark is 18 of 24.",
                onClick = { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) })
        }

        Group("Audio") {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.RecordVoiceOver, null, tint = c.textPrimary)
                Text("Read questions aloud automatically", style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                Switch(settings.audioAutoplay, viewModel::setAutoplay)
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Rounded.Speed, null, tint = c.textPrimary)
                    Text("Speed: ${"%.2f".format(settings.audioSpeed).trimEnd('0').trimEnd('.')}×", style = LituTheme.type.body, color = c.textPrimary)
                }
                Slider(
                    settings.audioSpeed, viewModel::setSpeed,
                    valueRange = UserSettings.MIN_AUDIO_SPEED..UserSettings.MAX_AUDIO_SPEED, steps = 2,
                )
            }
        }

        Group("Subscription") {
            if (s.isPro) {
                Item(null, "Manage subscription", "Change or cancel in Google Play", onClick = { uri.openUri(EntitlementRepository.MANAGE_SUBSCRIPTIONS_URL) })
            } else {
                Item(null, "See plans", "Unlock the full question bank and mock tests", onClick = onPaywall)
            }
            Item(null, "Restore purchases", onClick = viewModel::restore)
        }

        Group("About") {
            Item(null, "Version", "${s.versionName} · content v${s.contentVersion}")
            Item(null, "Disclaimer", LegalLinks.DISCLAIMER)
            Item(null, "Buy the official handbook", onClick = { uri.openUri(LegalLinks.OFFICIAL_HANDBOOK) })
            Item(null, "Privacy policy", onClick = { uri.openUri(LegalLinks.PRIVACY) })
            Item(null, "Terms of use", onClick = { uri.openUri(LegalLinks.TERMS) })
            Item(null, "Contact support", onClick = { uri.openUri(LegalLinks.SUPPORT) })
        }
    }

    when (dialog) {
        Dialog.DATE -> {
            val picker = rememberDatePickerState(
                initialSelectedDateMillis = settings.testDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
                selectableDates = object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis >= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                },
            )
            DatePickerDialog(
                onDismissRequest = { dialog = Dialog.NONE },
                confirmButton = {
                    LituButton("Save", {
                        viewModel.setTestDate(picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() })
                        dialog = Dialog.NONE
                    }, fillWidth = false)
                },
                dismissButton = {
                    LituButton("Not booked", { viewModel.setTestDate(null); dialog = Dialog.NONE }, variant = ButtonVariant.TEXT, fillWidth = false)
                },
            ) { DatePicker(picker) }
        }
        Dialog.GOAL -> AlertDialog(
            onDismissRequest = { dialog = Dialog.NONE },
            containerColor = c.surface,
            title = { Text("Daily goal") },
            text = {
                Column {
                    UserSettings.DAILY_GOAL_OPTIONS.forEach { g ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(settings.dailyGoal == g, role = Role.RadioButton) {
                                viewModel.setGoal(g); dialog = Dialog.NONE
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(settings.dailyGoal == g, null)
                            Text("$g questions (${DailyGoal.minutesFor(g)} min)", style = LituTheme.type.body, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
        )
        Dialog.TIME -> {
            val picker = rememberTimePickerState(settings.reminderTime.hour, settings.reminderTime.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { dialog = Dialog.NONE },
                containerColor = c.surface,
                title = { Text("Reminder time") },
                text = { TimePicker(picker) },
                confirmButton = {
                    LituButton("Save", { viewModel.setReminder(LocalTime.of(picker.hour, picker.minute), settings.remindersEnabled); dialog = Dialog.NONE }, fillWidth = false)
                },
                dismissButton = { LituButton("Cancel", { dialog = Dialog.NONE }, variant = ButtonVariant.TEXT, fillWidth = false) },
            )
        }
        Dialog.DELETE -> AlertDialog(
            onDismissRequest = { dialog = Dialog.NONE },
            containerColor = c.surface,
            title = { Text("Delete your data?") },
            text = {
                Text(
                    "This deletes your progress on this phone, your online backup and your account. It cannot be undone. " +
                        "Your subscription is managed by Google Play and is not cancelled.",
                    style = LituTheme.type.body,
                )
            },
            confirmButton = { LituButton("Delete", { dialog = Dialog.NONE; viewModel.deleteData(onDataDeleted) }, fillWidth = false, loading = s.busy) },
            dismissButton = { LituButton("Cancel", { dialog = Dialog.NONE }, variant = ButtonVariant.TEXT, fillWidth = false) },
        )
        Dialog.NONE -> Unit
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = LituTheme.type.label, color = LituTheme.colors.primary, modifier = Modifier.padding(top = 16.dp, start = 4.dp).semantics { heading() })
    Surface(shape = LituDimens.cardShape, color = LituTheme.colors.surface, border = androidx.compose.foundation.BorderStroke(1.dp, LituTheme.colors.outline)) {
        Column(content = content)
    }
}

@Composable
private fun Item(icon: ImageVector?, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    val c = LituTheme.colors
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.selectable(false, role = Role.Button, onClick = onClick) else base).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = c.textPrimary, modifier = Modifier.padding(end = 16.dp).size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = LituTheme.type.body, color = c.textPrimary)
            subtitle?.let { Text(it, style = LituTheme.type.caption, color = c.textSecondary) }
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
    }
}
