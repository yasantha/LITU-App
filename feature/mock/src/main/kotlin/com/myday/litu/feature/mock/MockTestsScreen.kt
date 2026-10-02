package com.myday.litu.feature.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.designsystem.component.BrandCard
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.EmptyState
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.PassFailBadge
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.illustration.LineIllustration
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.FinishMockUseCase
import com.myday.litu.core.domain.usecase.StartMockUseCase
import com.myday.litu.core.model.Exam
import com.myday.litu.core.model.MockExam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MockTestsState(
    val history: List<MockExam> = emptyList(),
    val nextNumber: Int = 1,
    val locked: Boolean = false,
    val hasOpenMock: Boolean = false,
)

@HiltViewModel
class MockTestsViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val startMock: StartMockUseCase,
    private val finishMock: FinishMockUseCase,
    entitlements: EntitlementRepository,
    config: ConfigRepository,
) : ViewModel() {
    private val open = kotlinx.coroutines.flow.MutableStateFlow(false)

    val state: StateFlow<MockTestsState> = combine(progress.observeFinishedMocks(), entitlements.isPro, config.config, open) { mocks, pro, cfg, hasOpen ->
        MockTestsState(
            history = mocks,
            nextNumber = mocks.size + 1,
            // Mock 1 is free (Remote Config free_mock_count).
            locked = !pro && !hasOpen && mocks.size >= cfg.freeMockCount,
            hasOpenMock = hasOpen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MockTestsState())

    fun refresh() = viewModelScope.launch {
        finishMock.finishExpired()
        open.value = progress.unfinishedMock() != null
    }

    fun start(onStarted: (String) -> Unit) = viewModelScope.launch { onStarted(startMock()) }
}

/** S14: next mock on a brand card, then history with date, score and Pass/Fail badge. */
@Composable
internal fun MockTestsScreen(
    onStart: (String) -> Unit,
    onResults: (String) -> Unit,
    onLocked: () -> Unit,
    viewModel: MockTestsViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val c = LituTheme.colors
    ScreenColumn(Modifier.padding(top = 24.dp)) {
        Text("Mock tests", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        BrandCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (s.hasOpenMock) "Mock test in progress" else "Mock test ${s.nextNumber}", style = LituTheme.type.title, color = c.onBrand)
                Text("${Exam.QUESTION_COUNT} questions · ${Exam.DURATION_MINUTES} min · ${Exam.PASS_MARK} to pass", style = LituTheme.type.body, color = c.onBrandMuted)
                LituButton(
                    when {
                        s.hasOpenMock -> "Resume mock test"
                        s.locked -> "Unlock more mock tests"
                        else -> "Start mock test"
                    },
                    {
                        when {
                            s.hasOpenMock -> viewModel.start(onStart)
                            s.locked -> onLocked()
                            else -> confirm = true
                        }
                    },
                    variant = ButtonVariant.AMBER,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        SectionHeader("History")
        if (s.history.isEmpty()) {
            EmptyState(Illustration.STUDY_CORNER, "No mock tests yet", "Your first mock test shows how ready you are.")
        }
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
        s.history.forEachIndexed { i, m ->
            LituCard(onClick = { onResults(m.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mock test ${s.history.size - i}", style = LituTheme.type.label, color = c.textPrimary)
                        Text(m.finishedAt?.atZone(ZoneId.systemDefault())?.format(fmt).orEmpty(), style = LituTheme.type.caption, color = c.textSecondary)
                    }
                    Text("${m.score ?: 0} / ${Exam.QUESTION_COUNT}", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.padding(end = 12.dp))
                    PassFailBadge(m.passed == true)
                }
            }
        }
    }
    if (confirm) StartConfirmation(onDismiss = { confirm = false }, onStart = { confirm = false; viewModel.start(onStart) })
}

/** S14b: shown every time a mock starts. */
@Composable
private fun StartConfirmation(onDismiss: () -> Unit, onStart: () -> Unit) {
    val c = LituTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        icon = { LineIllustration(Illustration.STUDY_CORNER, size = 96.dp) },
        title = { Text("Ready to start?", style = LituTheme.type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Rule(Icons.Rounded.VolumeOff, "Find a quiet place where you will not be disturbed.")
                Rule(Icons.Rounded.Timer, "You have 45 minutes and you cannot pause.")
                Rule(Icons.Rounded.Block, "You will not see any answers until the end.")
            }
        },
        confirmButton = { LituButton("Start", onStart, fillWidth = false) },
        dismissButton = { LituButton("Not yet", onDismiss, variant = ButtonVariant.TEXT, fillWidth = false) },
    )
}

@Composable
private fun Rule(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = LituTheme.colors.primary)
        Text(text, style = LituTheme.type.body, color = LituTheme.colors.textPrimary)
    }
}
