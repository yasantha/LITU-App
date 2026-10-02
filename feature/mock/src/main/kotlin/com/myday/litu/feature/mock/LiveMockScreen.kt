package com.myday.litu.feature.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myday.litu.core.designsystem.component.AnswerCard
import com.myday.litu.core.designsystem.component.AnswerState
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.CountdownChip
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LoadingBox
import com.myday.litu.core.designsystem.component.NavigatorSquare
import com.myday.litu.core.designsystem.component.NavigatorState
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.model.QuestionType

@Composable
internal fun LiveMockScreen(onFinished: (String) -> Unit, onLeave: () -> Unit, viewModel: LiveMockViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var navigator by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.finishedId) { s.finishedId?.let(onFinished) }
    BackHandler { confirmLeave = true }
    val q = s.question ?: return LoadingBox()
    val c = LituTheme.colors
    // TalkBack hears the time every 5 minutes, not every second.
    val announceMinutes = (s.remaining.toMinutes() + 1) / 5 * 5

    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CountdownChip(s.remaining)
            Text("${s.index + 1} / ${s.total}", style = LituTheme.type.label, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton({ navigator = true }) { Icon(Icons.Rounded.Apps, "Question navigator", tint = c.textPrimary) }
        }
        Text(
            "$announceMinutes minutes left",
            style = LituTheme.type.caption,
            color = c.background,
            modifier = Modifier.size(1.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(LituDimens.answerGap),
            ) {
                Text(q.stem, style = LituTheme.type.question, color = c.textPrimary, modifier = Modifier.semantics { heading() })
                if (q.type == QuestionType.MULTI) Text("Choose 2 answers", style = LituTheme.type.label, color = c.textSecondary)
                Spacer(Modifier.heightIn(min = 16.dp))
                s.options.forEach { o ->
                    AnswerCard(
                        o.label,
                        if (o.id in s.selected) AnswerState.SELECTED else AnswerState.DEFAULT,
                        onClick = { viewModel.select(o.id) },
                        multiSelect = q.type == QuestionType.MULTI,
                    )
                }
                val flagged = q.id in s.flagged
                LituButton(
                    if (flagged) "Flagged" else "Flag for later",
                    viewModel::toggleFlag,
                    variant = ButtonVariant.SECONDARY,
                    icon = if (flagged) Icons.Rounded.Flag else Icons.Outlined.Flag,
                    fillWidth = false,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Column(Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No feedback until you finish", style = LituTheme.type.caption, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Previous", viewModel::previous, Modifier.weight(1f), variant = ButtonVariant.SECONDARY, enabled = s.index > 0)
                if (s.index < s.total - 1) {
                    LituButton("Next", viewModel::next, Modifier.weight(1f))
                } else {
                    LituButton("Finish", { confirmFinish = true }, Modifier.weight(1f))
                }
            }
        }
    }

    if (navigator) {
        NavigatorSheet(
            s = s,
            onDismiss = { navigator = false },
            onJump = { navigator = false; viewModel.show(it) },
            onFinish = { navigator = false; confirmFinish = true },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            containerColor = c.surface,
            title = { Text("Finish the test?") },
            text = {
                Text(
                    if (s.unanswered > 0) "You have ${s.unanswered} unanswered questions. Unanswered questions count as wrong."
                    else "You have answered every question. You cannot change your answers after you finish.",
                    style = LituTheme.type.body,
                )
            },
            confirmButton = { LituButton("Finish test", { confirmFinish = false; viewModel.submit() }, fillWidth = false) },
            dismissButton = { LituButton("Back to test", { confirmFinish = false }, variant = ButtonVariant.TEXT, fillWidth = false) },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = c.surface,
            title = { Text("Leave the test?") },
            text = { Text("The timer keeps running, like the real test. You can come back from Mock tests.", style = LituTheme.type.body) },
            confirmButton = { LituButton("Back to test", { confirmLeave = false }, fillWidth = false) },
            dismissButton = { LituButton("Leave", { confirmLeave = false; onLeave() }, variant = ButtonVariant.TEXT, fillWidth = false) },
        )
    }
}

/** S15b: 24 squares in 6 columns, legend, unanswered warning. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigatorSheet(s: LiveMockState, onDismiss: () -> Unit, onJump: (Int) -> Unit, onFinish: () -> Unit) {
    val c = LituTheme.colors
    val m = s.mock ?: return
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Questions", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.semantics { heading() })
            m.questionIds.chunked(6).forEachIndexed { row, ids ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ids.forEachIndexed { col, id ->
                        val i = row * 6 + col
                        val state = when {
                            i == s.index -> NavigatorState.CURRENT
                            id in s.flagged -> NavigatorState.FLAGGED
                            !m.answers[id].isNullOrEmpty() -> NavigatorState.ANSWERED
                            else -> NavigatorState.UNANSWERED
                        }
                        NavigatorSquare(i + 1, state, { onJump(i) }, Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend("Answered", NavigatorState.ANSWERED)
                Legend("Not answered", NavigatorState.UNANSWERED)
                Legend("Flagged", NavigatorState.FLAGGED)
            }
            if (s.unanswered > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = c.warning)
                    Text("${s.unanswered} questions not answered yet.", style = LituTheme.type.body, color = c.textPrimary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Back to test", onDismiss, Modifier.weight(1f), variant = ButtonVariant.SECONDARY)
                LituButton("Finish test", onFinish, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Legend(label: String, state: NavigatorState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val c = LituTheme.colors
        val color = when (state) {
            NavigatorState.ANSWERED -> c.primaryContainer
            NavigatorState.FLAGGED -> c.amberContainer
            else -> c.surface
        }
        Box(Modifier.size(16.dp).background(color, LituDimens.chipShape).border(1.dp, if (state == NavigatorState.FLAGGED) c.accent else c.outline, LituDimens.chipShape))
        Text(label, style = LituTheme.type.caption, color = LituTheme.colors.textSecondary)
    }
}
