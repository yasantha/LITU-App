package com.myday.litu.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myday.litu.core.designsystem.component.EmptyState
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.LoadingBox
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.goal.DailyGoal
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.usecase.BuildPracticeSessionUseCase
import com.myday.litu.core.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@Serializable data object ReviewRoute

data class UpcomingItem(val stem: String, val due: String)

data class ReviewState(val loading: Boolean = true, val dueCount: Int = 0, val upcoming: List<UpcomingItem> = emptyList())

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val buildSession: BuildPracticeSessionUseCase,
    private val progress: ProgressRepository,
    private val content: ContentRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReviewState())
    val state = mutableState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        val due = buildSession.reviewQueue(Exam.REVIEW_SESSION_CAP).size
        val upcoming = progress.upcomingReviews(clock.instant(), 10)
        val questions = content.questions(upcoming.map { it.questionId }).associateBy { it.id }
        val today = LocalDate.now(clock)
        mutableState.value = ReviewState(
            loading = false,
            dueCount = due,
            upcoming = upcoming.mapNotNull { r ->
                val q = questions[r.questionId] ?: return@mapNotNull null
                val day = r.dueAt.atZone(clock.zone).toLocalDate()
                val label = when (ChronoUnit.DAYS.between(today, day)) {
                    0L -> "Later today"
                    1L -> "Tomorrow"
                    else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))
                }
                UpcomingItem(q.stem, label)
            },
        )
    }
}

fun NavGraphBuilder.reviewScreen(onBack: () -> Unit, onStartReview: () -> Unit, onMixedPractice: () -> Unit) {
    composable<ReviewRoute> { ReviewScreen(onBack, onStartReview, onMixedPractice) }
}

/** S13 review queue, and S13b when nothing is due. */
@Composable
internal fun ReviewScreen(
    onBack: () -> Unit,
    onStartReview: () -> Unit,
    onMixedPractice: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val c = LituTheme.colors
    if (s.loading) return LoadingBox()
    ScreenColumn(topBar = { LituTopBar("Review", onNav = onBack) }) {
        if (s.dueCount == 0) {
            EmptyState(
                Illustration.LIBRARY,
                "Nothing to review",
                "Questions you miss or flag come back here when they are due. Try some mixed practice for now.",
                actionLabel = "Mixed practice",
                onAction = onMixedPractice,
            )
        } else {
            LituCard(color = c.amberContainer, border = false) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Replay, null, tint = c.warning)
                    Column(Modifier.weight(1f)) {
                        Text("${s.dueCount} questions due", style = LituTheme.type.title, color = c.textPrimary)
                        Text("About ${DailyGoal.estimatedMinutes(s.dueCount)} minutes", style = LituTheme.type.caption, color = c.textSecondary)
                    }
                }
                LituButton("Start review", onStartReview, Modifier.padding(top = 16.dp))
            }
        }
        if (s.upcoming.isNotEmpty()) {
            SectionHeader("Coming up")
            LituCard {
                s.upcoming.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.outline)
                    Text(item.stem, style = LituTheme.type.body, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.due, style = LituTheme.type.caption, color = c.textSecondary)
                }
            }
        }
    }
}
