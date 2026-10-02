package com.myday.litu.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.myday.litu.core.designsystem.component.ChapterTile
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LockBadge
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.ModeTile
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.Mastery
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class HubChapter(val chapter: Chapter, val mastery: Mastery, val firstSectionId: String?)

data class PracticeHubState(
    val chapters: List<HubChapter> = emptyList(),
    val dueCount: Int = 0,
    val isPro: Boolean = false,
)

@HiltViewModel
class PracticeHubViewModel @Inject constructor(
    private val overview: ProgressOverviewUseCase,
    private val progress: ProgressRepository,
    entitlements: EntitlementRepository,
    private val clock: Clock,
) : ViewModel() {
    private val data = MutableStateFlow(PracticeHubState())

    val state: StateFlow<PracticeHubState> = combine(data, entitlements.isPro) { d, pro -> d.copy(isPro = pro) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeHubState())

    fun refresh() = viewModelScope.launch {
        val o = overview()
        val due = progress.dueReviews(clock.instant(), Exam.REVIEW_SESSION_CAP).size
        data.value = PracticeHubState(
            chapters = o.chapters.map { ch ->
                HubChapter(ch, o.chapterMastery[ch.id] ?: Mastery(0, 0), o.sections.filter { it.chapterId == ch.id }.minByOrNull { it.sort }?.id)
            },
            dueCount = due,
        )
    }
}

@Composable
internal fun PracticeHubScreen(
    onReview: () -> Unit,
    onStart: (QuestionSessionRoute) -> Unit,
    onChapter: (String) -> Unit,
    onNotes: (String) -> Unit,
    onLocked: () -> Unit,
    viewModel: PracticeHubViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val c = LituTheme.colors
    ScreenColumn(Modifier.padding(top = 24.dp)) {
        Text("Practice", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(c.coralContainer, onReview, Modifier.weight(1f).heightIn(min = 104.dp)) {
                Icon(Icons.Rounded.Replay, null, tint = c.onCoralContainer)
                Text("Review", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
                Text("${s.dueCount} due", style = LituTheme.type.caption, color = c.textSecondary)
            }
            ModeTile(
                c.amberContainer,
                { if (s.isPro) onStart(QuestionSessionRoute.mixed()) else onLocked() },
                Modifier.weight(1f).heightIn(min = 104.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shuffle, null, tint = c.warning, modifier = Modifier.weight(1f, fill = false))
                    if (!s.isPro) LockBadge(Modifier.padding(start = 8.dp))
                }
                Text("Mixed", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
                Text("${Exam.MIXED_PRACTICE_COUNT} random", style = LituTheme.type.caption, color = c.textSecondary)
            }
        }
        SectionHeader("Chapters")
        s.chapters.forEach { h -> ChapterRow(h, s.isPro, onChapter, onNotes, onLocked) }
    }
}

@Composable
private fun ChapterRow(h: HubChapter, isPro: Boolean, onChapter: (String) -> Unit, onNotes: (String) -> Unit, onLocked: () -> Unit) {
    val c = LituTheme.colors
    val studyOnly = !h.chapter.inTest
    LituCard(onClick = {
        when {
            !isPro -> onLocked()
            studyOnly -> h.firstSectionId?.let(onNotes)
            else -> onChapter(h.chapter.id)
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChapterTile(h.chapter.number)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(h.chapter.title, style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.weight(1f))
                    if (!isPro) LockBadge()
                }
                if (studyOnly) {
                    Text("Study only · Read the notes – not in the test", style = LituTheme.type.caption, color = c.textSecondary)
                } else {
                    MasteryBar(h.mastery.fraction, c.chapter(h.chapter.number), Modifier.fillMaxWidth())
                }
            }
        }
    }
}
