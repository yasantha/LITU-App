package com.myday.litu.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myday.litu.core.designsystem.component.AnswerCard
import com.myday.litu.core.designsystem.component.AnswerState
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.EmptyState
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituProgressBar
import com.myday.litu.core.designsystem.component.LoadingBox
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.QuestionReport
import com.myday.litu.core.domain.repository.ReportReason
import com.myday.litu.core.model.QuestionType

@Composable
internal fun QuestionSessionScreen(
    onClose: () -> Unit,
    onDone: () -> Unit,
    onSampleFinished: () -> Unit,
    onPractiseAgain: (QuestionSessionRoute) -> Unit,
    onLocked: () -> Unit,
    viewModel: QuestionSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playing by viewModel.audioPlaying.collectAsStateWithLifecycle()
    when (val s = state) {
        SessionUiState.Loading -> LoadingBox()
        SessionUiState.Locked -> LaunchedEffect(Unit) { onLocked() }
        SessionUiState.Empty -> Column(Modifier.fillMaxSize().background(LituTheme.colors.background).statusBarsPadding()) {
            EmptyState(Illustration.STUDY_CORNER, "No questions here yet", "Try another topic or mixed practice.", actionLabel = "Back", onAction = onClose)
        }
        is SessionUiState.Answering -> AnsweringContent(
            s = s,
            playing = playing,
            onClose = onClose,
            onSelect = viewModel::select,
            onCheck = viewModel::check,
            onNext = viewModel::next,
            onFlag = viewModel::toggleFlag,
            onAudio = viewModel::toggleAudio,
            onReport = viewModel::report,
        )
        is SessionUiState.Summary -> if (s.isSample) {
            SampleResults(s, onSeePlans = onSampleFinished)
        } else {
            SessionSummary(s, onDone = onDone, onPractiseAgain = { onPractiseAgain(QuestionSessionRoute.mixed()) })
        }
    }
}

@Composable
private fun AnsweringContent(
    s: SessionUiState.Answering,
    playing: String?,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onFlag: () -> Unit,
    onAudio: (explanation: Boolean) -> Unit,
    onReport: (ReportReason, String) -> Unit,
) {
    val c = LituTheme.colors
    var showReport by rememberSaveable { mutableStateOf(false) }
    val questionAudio = playing == "q:${s.question.id}"
    Column(Modifier.fillMaxSize().background(c.background)) {
        // Header: close, "5 of 20", audio, flag; progress bar.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Rounded.Close, "Close") }
            Text(
                if (s.isSample) "Free sample · ${s.index + 1} of ${s.total}" else "${s.index + 1} of ${s.total}",
                style = LituTheme.type.label,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton({ onAudio(false) }) {
                Icon(
                    if (questionAudio) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                    if (questionAudio) "Stop reading" else "Read question aloud",
                    tint = c.textPrimary,
                )
            }
            IconButton(onFlag) {
                Icon(
                    if (s.flagged) Icons.Rounded.Flag else Icons.Outlined.Flag,
                    if (s.flagged) "Remove flag" else "Flag for review",
                    tint = if (s.flagged) c.accent else c.textPrimary,
                )
            }
        }
        LituProgressBar((s.index + 1f) / s.total, Modifier.padding(horizontal = 16.dp), height = 6.dp)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(LituDimens.answerGap),
            ) {
                Text(s.question.stem, style = LituTheme.type.question, color = c.textPrimary, modifier = Modifier.semantics { heading() })
                if (s.question.type == QuestionType.MULTI) {
                    Text("Choose 2 answers", style = LituTheme.type.label, color = c.textSecondary)
                }
                // Thumb-first: answers sit in the lower part of the screen.
                Spacer(Modifier.heightIn(min = 16.dp).weight(1f, fill = false))
                s.options.forEach { o ->
                    val chosen = o.id in s.selected
                    val state = when {
                        !s.checked -> if (chosen) AnswerState.SELECTED else AnswerState.DEFAULT
                        o.isCorrect && chosen -> AnswerState.CORRECT
                        o.isCorrect -> AnswerState.CORRECT_NOT_CHOSEN
                        chosen -> AnswerState.WRONG
                        else -> AnswerState.DISABLED
                    }
                    AnswerCard(o.label, state, onClick = { onSelect(o.id) }, multiSelect = s.question.type == QuestionType.MULTI)
                }
            }
        }

        if (!s.checked) {
            Column(Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally).navigationBarsPadding().padding(16.dp)) {
                LituButton("Check", onCheck, enabled = s.canCheck)
            }
        }
        AnimatedVisibility(s.checked, enter = slideInVertically { it }) {
            FeedbackPanel(
                correct = s.wasCorrect == true,
                explanation = s.question.explanation,
                handbookRef = s.question.handbookRef,
                isLast = s.isLast,
                explanationPlaying = playing == "e:${s.question.id}",
                onListen = { onAudio(true) },
                onNext = onNext,
                onReport = { showReport = true },
            )
        }
    }
    if (showReport) {
        ReportSheet(questionId = s.question.id, onDismiss = { showReport = false }, onSend = { r, note -> onReport(r, note); showReport = false })
    }
}

/** Feedback sheet: verdict with icon and text, explanation, handbook reference, Report and Next. */
@Composable
private fun FeedbackPanel(
    correct: Boolean,
    explanation: String,
    handbookRef: String,
    isLast: Boolean,
    explanationPlaying: Boolean,
    onListen: () -> Unit,
    onNext: () -> Unit,
    onReport: () -> Unit,
) {
    val c = LituTheme.colors
    Surface(shape = LituDimens.sheetShape, color = c.surface, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                .heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp).background(c.outline, LituDimens.chipShape))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Icon(if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (correct) c.success else c.error)
                Text(if (correct) "Correct" else "Not quite", style = LituTheme.type.title, color = if (correct) c.success else c.error, modifier = Modifier.weight(1f))
                IconButton(onListen) {
                    Icon(
                        if (explanationPlaying) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                        if (explanationPlaying) "Stop reading" else "Read explanation aloud",
                        tint = c.textPrimary,
                    )
                }
            }
            Text(explanation, style = LituTheme.type.body, color = c.textPrimary)
            Text(handbookRef, style = LituTheme.type.caption, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Report", onReport, variant = ButtonVariant.TEXT, icon = Icons.Outlined.Flag, fillWidth = false)
                LituButton(if (isLast) "Finish" else "Next", onNext, Modifier.weight(1f))
            }
        }
    }
}

/** S11b: report a question. Works offline; queued and sent with the next sync. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(questionId: String, onDismiss: () -> Unit, onSend: (ReportReason, String) -> Unit) {
    val c = LituTheme.colors
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    val reasons = listOf(
        ReportReason.WRONG to "The answer is wrong",
        ReportReason.UNCLEAR to "The question is unclear",
        ReportReason.TYPO to "Spelling or grammar mistake",
        ReportReason.OTHER to "Something else",
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Report a question", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.semantics { heading() })
            Text("Question $questionId", style = LituTheme.type.caption, color = c.textSecondary)
            reasons.forEach { (r, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(reason == r, role = Role.RadioButton) { reason = r },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == r, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = LituTheme.type.body, color = c.textPrimary)
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(QuestionReport.MAX_COMMENT) },
                label = { Text("Add a note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Text("Reports are sent when you are next online.", style = LituTheme.type.caption, color = c.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Cancel", onDismiss, Modifier.weight(1f), variant = ButtonVariant.SECONDARY)
                LituButton("Send report", { reason?.let { onSend(it, note) } }, Modifier.weight(1f), enabled = reason != null)
            }
        }
    }
}
