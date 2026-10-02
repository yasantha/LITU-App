package com.myday.litu.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituFilterChip
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.TimerRing
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@Serializable data object TimerRoute

data class TimerState(
    val lengthMinutes: Int = DEFAULT_MINUTES,
    val remainingMs: Long = DEFAULT_MINUTES * 60_000L,
    val running: Boolean = false,
    val finished: Boolean = false,
) {
    val progress: Float get() = 1f - remainingMs.toFloat() / (lengthMinutes * 60_000L)

    companion object {
        const val DEFAULT_MINUTES = 15
        val LENGTHS = listOf(10, 15, 25)
    }
}

/**
 * Focus session from Daily Focus, ending with a 5-question quiz. The timer counts down to a
 * wall-clock end time and runs only while the screen is open: no background service (spec 14).
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TimerState())
    val state = mutableState.asStateFlow()
    private var ticker: Job? = null
    private var endAt: Long? = savedState["endAt"]

    init {
        if (endAt != null) start()
    }

    fun setLength(minutes: Int) {
        if (mutableState.value.running) return
        mutableState.value = TimerState(lengthMinutes = minutes, remainingMs = minutes * 60_000L)
    }

    fun toggle() = if (mutableState.value.running) pause() else start()

    private fun start() {
        val end = endAt ?: (clock.millis() + mutableState.value.remainingMs).also { endAt = it; savedState["endAt"] = it }
        mutableState.update { it.copy(running = true, finished = false) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val left = (end - clock.millis()).coerceAtLeast(0)
                mutableState.update { it.copy(remainingMs = left) }
                if (left == 0L) return@launch complete()
                delay(250)
            }
        }
    }

    private fun pause() {
        ticker?.cancel()
        endAt = null
        savedState["endAt"] = null
        mutableState.update { it.copy(running = false) }
    }

    fun reset() {
        pause()
        setLength(mutableState.value.lengthMinutes)
    }

    fun skip() = viewModelScope.launch { complete() }

    private suspend fun complete() {
        ticker?.cancel()
        endAt = null
        savedState["endAt"] = null
        val s = mutableState.value
        val minutes = ((s.lengthMinutes * 60_000L - s.remainingMs) / 60_000L).toInt()
        if (minutes > 0) progress.addToDailyStat(LocalDate.now(clock), 0, 0, minutes, settings.current().dailyGoal)
        mutableState.update { it.copy(running = false, finished = true) }
    }

    fun dismissFinished() = mutableState.update { TimerState(lengthMinutes = it.lengthMinutes, remainingMs = it.lengthMinutes * 60_000L) }
}

fun NavGraphBuilder.timerScreen(onBack: () -> Unit, onQuiz: () -> Unit) {
    composable<TimerRoute> { TimerScreen(onBack, onQuiz) }
}

/** S19: large circular timer, start/pause, reset, skip, length chips 10/15/25. */
@Composable
internal fun TimerScreen(onBack: () -> Unit, onQuiz: () -> Unit, viewModel: TimerViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    val totalSec = (s.remainingMs + 999) / 1000
    val label = "%d:%02d".format(totalSec / 60, totalSec % 60)
    ScreenColumn(topBar = { LituTopBar("Study timer", onNav = onBack) }, spacing = 24.dp) {
        Text("Focus on one topic. When the time is up, test yourself with a quick quiz.",
            style = LituTheme.type.body, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        TimerRing(s.progress, Modifier.align(Alignment.CenterHorizontally)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "${totalSec / 60} minutes left"
            }) {
                Text(label, style = LituTheme.type.display, color = c.textPrimary)
                Text(if (s.running) "Focusing" else "Ready", style = LituTheme.type.label, color = c.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            TimerState.LENGTHS.forEach { m -> LituFilterChip("$m min", s.lengthMinutes == m, { viewModel.setLength(m) }) }
        }
        LituButton(if (s.running) "Pause" else "Start", viewModel::toggle, icon = if (s.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LituButton("Reset", viewModel::reset, Modifier.weight(1f), variant = ButtonVariant.SECONDARY, icon = Icons.Rounded.Replay)
            LituButton("Skip", viewModel::skip, Modifier.weight(1f), variant = ButtonVariant.SECONDARY, icon = Icons.Rounded.SkipNext)
        }
    }
    if (s.finished) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinished,
            containerColor = c.surface,
            title = { Text("Session complete", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) },
            text = { Text("Quick 5-question quiz?", style = LituTheme.type.body) },
            confirmButton = { LituButton("Start quiz", { viewModel.dismissFinished(); onQuiz() }, fillWidth = false) },
            dismissButton = { LituButton("Not now", viewModel::dismissFinished, variant = ButtonVariant.TEXT, fillWidth = false) },
        )
    }
}
