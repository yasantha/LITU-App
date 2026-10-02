package com.myday.litu.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.ChapterTile
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.usecase.Mastery
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChapterDetailState(
    val chapter: Chapter? = null,
    val mastery: Mastery = Mastery(0, 0),
    val sections: List<Pair<Section, Mastery>> = emptyList(),
)

@HiltViewModel
class ChapterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val overview: ProgressOverviewUseCase,
) : ViewModel() {
    private val chapterId = savedStateHandle.toRoute<ChapterDetailRoute>().chapterId
    private val mutableState = MutableStateFlow(ChapterDetailState())
    val state = mutableState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        val o = overview()
        mutableState.value = ChapterDetailState(
            chapter = o.chapter(chapterId),
            mastery = o.chapterMastery[chapterId] ?: Mastery(0, 0),
            sections = o.sections.filter { it.chapterId == chapterId }.sortedBy { it.sort }
                .map { it to (o.sectionMastery[it.id] ?: Mastery(0, 0)) },
        )
    }
}

/** S09: chapter detail. Tap a section to practise only that section. */
@Composable
internal fun ChapterDetailScreen(
    onBack: () -> Unit,
    onStart: (QuestionSessionRoute) -> Unit,
    onNotes: (String) -> Unit,
    viewModel: ChapterDetailViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val c = LituTheme.colors
    val chapter = s.chapter ?: return
    ScreenColumn(topBar = { LituTopBar("Chapter ${chapter.number}", onNav = onBack) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChapterTile(chapter.number)
            Column {
                Text(chapter.title, style = LituTheme.type.headline, color = c.textPrimary)
                Text("${s.mastery.percent}% mastered", style = LituTheme.type.label, color = c.textSecondary)
            }
        }
        MasteryBar(s.mastery.fraction, c.chapter(chapter.number))
        LituButton("Practise this chapter", { onStart(QuestionSessionRoute.chapter(chapter.id)) })
        s.sections.firstOrNull()?.let { (first, _) ->
            LituButton("Read notes", { onNotes(first.id) }, variant = ButtonVariant.SECONDARY)
        }
        SectionHeader("Sections")
        s.sections.forEach { (section, mastery) ->
            LituCard(onClick = { onStart(QuestionSessionRoute.section(section.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = LituTheme.type.label, color = c.textPrimary)
                        Text("${mastery.total} questions", style = LituTheme.type.caption, color = c.textSecondary)
                        MasteryBar(mastery.fraction, c.chapter(chapter.number), Modifier.fillMaxWidth())
                    }
                    IconButton({ onNotes(section.id) }, Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, "Read notes for ${section.title}", tint = c.primary)
                    }
                }
            }
        }
    }
}
