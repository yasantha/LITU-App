package com.myday.litu.feature.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myday.litu.core.designsystem.component.ChapterTile
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.PassFailBadge
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.ObserveTodayUseCase
import com.myday.litu.core.domain.usecase.ProgressOverview
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.domain.usecase.TodayProgress
import com.myday.litu.core.domain.streak.StreakInfo
import com.myday.litu.core.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

@Serializable data object ProgressRoute

data class ProgressState(
    val overview: ProgressOverview? = null,
    val today: TodayProgress = TodayProgress(0, 20, StreakInfo.EMPTY, emptyList()),
    val totalAnswered: Int = 0,
    val minutesThisWeek: Int = 0,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val overviewUseCase: ProgressOverviewUseCase,
    progress: ProgressRepository,
    observeToday: ObserveTodayUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val overview = MutableStateFlow<ProgressOverview?>(null)

    val state = combine(overview, observeToday(), progress.observeTotalAnswered()) { o, today, total ->
        val weekStart = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ProgressState(o, today, total, today.stats.filter { !it.day.isBefore(weekStart) }.sumOf { it.studyMinutes })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun refresh() = viewModelScope.launch { overview.value = overviewUseCase() }
}

fun NavGraphBuilder.progressScreen(onMockResults: (String) -> Unit) {
    composable<ProgressRoute> { ProgressScreen(onMockResults) }
}

/** S18: readiness over time, streak, minutes, totals, mastery by chapter, mock history. */
@Composable
internal fun ProgressScreen(onMockResults: (String) -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val c = LituTheme.colors
    val o = s.overview
    ScreenColumn(Modifier.padding(top = 24.dp)) {
        Text("Progress", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        LituCard {
            Text("Readiness over time", style = LituTheme.type.title, color = c.textPrimary)
            val history = o?.readinessHistory.orEmpty()
            if (history.isEmpty()) {
                Text("Take a mock test to start tracking your readiness.", style = LituTheme.type.body, color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
            } else {
                ReadinessChart(history, Modifier.padding(top = 8.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Streak", if (s.today.streak.current == 1) "1 day" else "${s.today.streak.current} days", "Best ${s.today.streak.best}", Modifier.weight(1f))
            Stat("This week", "${s.minutesThisWeek} min", "Study time", Modifier.weight(1f))
            Stat("Answered", "${s.totalAnswered}", "All time", Modifier.weight(1f))
        }
        SectionHeader("Mastery by chapter")
        LituCard {
            o?.chapters?.forEach { ch ->
                val m = o.chapterMastery[ch.id]
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChapterTile(ch.number, size = 32.dp)
                    Column(Modifier.weight(1f)) {
                        Text(ch.title, style = LituTheme.type.label, color = c.textPrimary)
                        MasteryBar(m?.fraction ?: 0f, c.chapter(ch.number))
                    }
                }
            }
        }
        SectionHeader("Mock history")
        if (o?.recentMocks.isNullOrEmpty()) {
            Text("No mock tests yet.", style = LituTheme.type.body, color = c.textSecondary)
        } else {
            LituCard {
                val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
                o!!.recentMocks.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(color = c.outline, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onMockResults(m.id) }.padding(vertical = 4.dp),
                    ) {
                        Text(
                            m.finishedAt?.atZone(ZoneId.systemDefault())?.format(fmt).orEmpty(),
                            style = LituTheme.type.body,
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${m.score ?: 0} / ${Exam.QUESTION_COUNT}", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.width(72.dp))
                        PassFailBadge(m.passed == true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, modifier: Modifier) {
    val c = LituTheme.colors
    LituCard(modifier) {
        Text(label, style = LituTheme.type.caption, color = c.textSecondary)
        Text(value, style = LituTheme.type.title, color = c.textPrimary)
        Text(sub, style = LituTheme.type.caption, color = c.textSecondary)
    }
}
