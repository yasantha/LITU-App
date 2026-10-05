package com.myday.litu.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.myday.litu.core.audio.ReadAloud
import com.myday.litu.core.audio.ReadAloudItem
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.ChapterTile
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.LoadingBox
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.plan.KeyFactMatcher
import com.myday.litu.core.domain.plan.StudyPlanUseCase
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.Note
import com.myday.litu.core.model.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** @param questionId set when opened from "Read about this" after a wrong answer. */
@Serializable data class NotesRoute(val sectionId: String, val questionId: String? = null)

data class NotesState(
    val chapter: Chapter? = null,
    val section: Section? = null,
    val note: Note? = null,
    val previous: Section? = null,
    val next: Section? = null,
    val missed: MissedFact? = null,
)

/** The question the learner got wrong, its answer, and the key fact that explains it. */
data class MissedFact(val stem: String, val answer: String, val fact: String?)

@HiltViewModel
class NotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val settings: SettingsRepository,
    private val readAloud: ReadAloud,
    private val studyPlan: StudyPlanUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotesState())
    val state = mutableState.asStateFlow()
    val playing = readAloud.playing

    private val questionId = savedStateHandle.toRoute<NotesRoute>().questionId

    init {
        load(savedStateHandle.toRoute<NotesRoute>().sectionId)
    }

    fun load(sectionId: String) = viewModelScope.launch {
        readAloud.stop()
        val section = content.section(sectionId) ?: return@launch
        val siblings = content.sections(section.chapterId)
        val i = siblings.indexOfFirst { it.id == sectionId }
        val note = content.note(sectionId)
        val question = questionId?.let { content.question(it) }?.takeIf { it.sectionId == sectionId }
        mutableState.value = NotesState(
            chapter = content.chapter(section.chapterId),
            section = section,
            note = note,
            previous = siblings.getOrNull(i - 1),
            next = siblings.getOrNull(i + 1),
            missed = question?.let { q ->
                MissedFact(
                    stem = q.stem,
                    answer = q.options.filter { it.isCorrect }.joinToString(" and ") { it.label },
                    fact = note?.keyFacts?.let { facts -> KeyFactMatcher.bestMatch(q, facts)?.let(facts::get) },
                )
            },
        )
        studyPlan.markNoteRead(sectionId)
    }

    fun toggleAudio() = viewModelScope.launch {
        val s = state.value
        val note = s.note ?: return@launch
        readAloud.toggle(ReadAloudItem.NoteItem(note, s.section?.title.orEmpty()), settings.current().audioSpeed)
    }

    override fun onCleared() = readAloud.stop()
}

fun NavGraphBuilder.notesScreen(onBack: () -> Unit, onTestSection: (String) -> Unit) {
    composable<NotesRoute> { NotesScreen(onBack, onTestSection) }
}

/** S10: original plain-English summary, key-fact boxes, audio and "Test yourself". */
@Composable
internal fun NotesScreen(onBack: () -> Unit, onTestSection: (String) -> Unit, viewModel: NotesViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val playing by viewModel.playing.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    val uri = LocalUriHandler.current
    val section = s.section ?: return LoadingBox()
    val chapter = s.chapter
    val audioOn = playing == "n:${section.id}"
    ScreenColumn(
        topBar = {
            LituTopBar("Study notes", onNav = onBack) {
                IconButton(viewModel::toggleAudio) {
                    Icon(if (audioOn) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp, if (audioOn) "Stop reading" else "Read notes aloud", tint = c.textPrimary)
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            chapter?.let { ChapterTile(it.number) }
            Column {
                chapter?.let { Text("Chapter ${it.number} · ${it.title}", style = LituTheme.type.caption, color = c.textSecondary) }
                Text(section.title, style = LituTheme.type.headline, color = c.textPrimary)
            }
        }
        s.missed?.let { m ->
            LituCard(color = c.errorContainer, border = false) {
                Text("The fact you missed", style = LituTheme.type.title, color = c.textPrimary)
                Text(m.stem, style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 4.dp))
                Text("Answer: ${m.answer}", style = LituTheme.type.label, color = c.success, modifier = Modifier.padding(top = 4.dp))
                m.fact?.let { Text("Key fact: $it", style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        val note = s.note
        if (note == null) {
            Text("Notes for this section are coming soon.", style = LituTheme.type.body, color = c.textSecondary)
        } else {
            note.bodyMarkdown.split("\n\n").forEach { para ->
                Text(markdownBold(para.trim()), style = LituTheme.type.body, color = c.textPrimary)
            }
            LituCard(color = c.amberContainer, border = false) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Key, null, tint = c.warning)
                    Text("Key facts", style = LituTheme.type.title, color = c.textPrimary)
                }
                note.keyFacts.forEach { fact ->
                    val isMissed = fact == s.missed?.fact
                    Text(
                        "• $fact",
                        style = if (isMissed) LituTheme.type.label else LituTheme.type.body,
                        color = c.textPrimary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (chapter?.inTest == true) {
            LituButton(if (s.missed != null) "Try questions on this section" else "Test yourself on this section", { onTestSection(section.id) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.previous?.let { p -> LituButton("Previous", { viewModel.load(p.id) }, Modifier.weight(1f), variant = ButtonVariant.SECONDARY) }
            s.next?.let { n -> LituButton("Next section", { viewModel.load(n.id) }, Modifier.weight(1f), variant = ButtonVariant.SECONDARY) }
        }
        LituButton(
            "Buy the official handbook",
            { uri.openUri("https://www.gov.uk/life-in-the-uk-test/prepare-for-your-test") },
            variant = ButtonVariant.TEXT,
        )
    }
}

/** Notes use a small Markdown subset: **bold** inside paragraphs. */
private fun markdownBold(text: String): AnnotatedString = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
