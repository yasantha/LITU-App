package com.myday.litu.feature.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.LoadingBox
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.PassFailBadge
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.TopBarNav
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.model.Chapter
import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

data class MockResultsState(
    val loaded: Boolean = false,
    val number: Int = 0,
    val score: Int = 0,
    val passed: Boolean = false,
    val duration: Duration? = null,
    val chapters: List<Pair<Chapter, ChapterScore>> = emptyList(),
    val readiness: Int? = null,
    val weakestChapters: List<Chapter> = emptyList(),
    val canReview: Boolean = false,
)

@HiltViewModel
class MockResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progress: ProgressRepository,
    private val overview: ProgressOverviewUseCase,
) : ViewModel() {
    val mockId = savedStateHandle.toRoute<MockResultsRoute>().mockId
    private val mutableState = MutableStateFlow(MockResultsState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val mock = progress.mock(mockId) ?: return@launch
            val o = overview()
            val all = progress.recentFinishedMocks(Int.MAX_VALUE)
            val number = all.size - all.indexOfFirst { it.id == mockId }
            // Weakest chapters in this mock first, then overall mastery.
            val ranked = mock.chapterBreakdown.entries.sortedBy { it.value.correct.toDouble() / it.value.total.coerceAtLeast(1) }
                .mapNotNull { o.chapter(it.key) }
            mutableState.value = MockResultsState(
                loaded = true,
                number = number,
                score = mock.score ?: 0,
                passed = mock.passed == true,
                duration = mock.finishedAt?.let { Duration.between(mock.startedAt, it) }?.takeIf { !it.isZero },
                chapters = mock.chapterBreakdown.mapNotNull { (id, sc) -> o.chapter(id)?.let { it to sc } }.sortedBy { it.first.number },
                readiness = o.readiness,
                weakestChapters = (ranked + o.weakestChapters(2)).distinctBy { it.id }.take(2),
                canReview = mock.questionIds.isNotEmpty(),
            )
        }
    }
}

/** S16 results, or S16b when not passed. Never blames; always offers the next step. */
@Composable
internal fun MockResultsScreen(
    onDone: () -> Unit,
    onReviewAnswers: (String) -> Unit,
    onPractiseChapters: (List<String>) -> Unit,
    onPassed: () -> Unit,
    viewModel: MockResultsViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val c = LituTheme.colors
    BackHandler(onBack = onDone)
    if (!s.loaded) return LoadingBox()
    // A passed mock is a good moment to ask for a rating (once; the app limits how often).
    androidx.compose.runtime.LaunchedEffect(s.passed) { if (s.passed) onPassed() }
    val mockId = viewModel.mockId
    ScreenColumn(
        topBar = { LituTopBar("", nav = TopBarNav.CLOSE, onNav = onDone) },
        bottomBar = {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.passed) {
                    if (s.canReview) LituButton("Review answers", { onReviewAnswers(mockId) })
                    LituButton("Done", onDone, variant = ButtonVariant.SECONDARY)
                } else {
                    LituButton("Practise your 2 weakest chapters", { onPractiseChapters(s.weakestChapters.map { it.id }) })
                    if (s.canReview) LituButton("Review answers", { onReviewAnswers(mockId) }, variant = ButtonVariant.SECONDARY)
                }
            }
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mock test ${s.number}", style = LituTheme.type.caption, color = c.textSecondary)
            Text("${s.score} / ${Exam.QUESTION_COUNT}", style = LituTheme.type.display.copy(fontSize = LituTheme.type.display.fontSize * 1.4f),
                color = c.textPrimary, modifier = Modifier.semantics { heading() })
            PassFailBadge(s.passed)
            val time = s.duration?.let { "Time ${it.toMinutes()}:%02d".format(it.seconds % 60) }
            val readiness = s.readiness?.let { "Readiness now $it%" }
            Text(listOfNotNull(time, readiness).joinToString(" · "), style = LituTheme.type.caption, color = c.textSecondary, textAlign = TextAlign.Center)
        }
        if (!s.passed) {
            val away = (Exam.PASS_MARK - s.score).coerceAtLeast(1)
            Text("You needed ${Exam.PASS_MARK} to pass.", style = LituTheme.type.body, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            LituCard(color = c.primaryContainer, border = false) {
                Text(if (away == 1) "You were 1 question away" else "You were $away questions away", style = LituTheme.type.title, color = c.textPrimary)
                Text("Many people pass after a few more mock tests. Focus on your weakest chapters, then try again.",
                    style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (s.chapters.isNotEmpty()) {
            LituCard {
                Text("By chapter", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
                s.chapters.forEach { (ch, sc) ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ch.title, style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.width(130.dp))
                        MasteryBar(sc.correct.toFloat() / sc.total.coerceAtLeast(1), c.chapter(ch.number), Modifier.weight(1f), showPercent = false)
                        Text("${sc.correct}/${sc.total}", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        s.weakestChapters.firstOrNull()?.takeIf { s.passed }?.let { weakest ->
            LituCard(color = c.amberContainer, border = false) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = c.warning)
                    Text("Practise ${weakest.title} next. It is your weakest chapter.", style = LituTheme.type.body, color = c.textPrimary)
                }
            }
        }
    }
}
