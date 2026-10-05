package com.myday.litu.feature.mock

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.myday.litu.core.designsystem.component.EmptyState
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituFilterChip
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ContentRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.model.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReviewFilter(val label: String) { ALL("All"), WRONG("Wrong"), FLAGGED("Flagged") }

data class ReviewedAnswer(val number: Int, val question: Question, val chosen: List<String>, val correct: Boolean, val flagged: Boolean)

@HiltViewModel
class AnswerReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progress: ProgressRepository,
    private val content: ContentRepository,
) : ViewModel() {
    private val mockId = savedStateHandle.toRoute<AnswerReviewRoute>().mockId
    private val mutableItems = MutableStateFlow<List<ReviewedAnswer>>(emptyList())
    val items = mutableItems.asStateFlow()

    init {
        viewModelScope.launch {
            val mock = progress.mock(mockId) ?: return@launch
            val questions = content.questions(mock.questionIds).associateBy { it.id }
            mutableItems.value = mock.questionIds.mapIndexedNotNull { i, id ->
                val q = questions[id] ?: return@mapIndexedNotNull null
                val chosen = mock.answers[id].orEmpty()
                ReviewedAnswer(i + 1, q, chosen, q.isCorrect(chosen), progress.reviewState(id)?.flagged == true)
            }
        }
    }
}

/** S17: filter chips All / Wrong / Flagged; expandable answers with explanation. */
@Composable
internal fun AnswerReviewScreen(
    onBack: () -> Unit,
    onReadAbout: (sectionId: String, questionId: String) -> Unit,
    viewModel: AnswerReviewViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.ALL) }
    val c = LituTheme.colors
    ScreenColumn(topBar = { LituTopBar("Answer review", onNav = onBack) }, spacing = 8.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewFilter.entries.forEach { f -> LituFilterChip(f.label, filter == f, { filter = f }) }
        }
        Text("Wrong answers have been added to your review queue.", style = LituTheme.type.caption, color = c.textSecondary)
        val shown = items.filter {
            when (filter) {
                ReviewFilter.ALL -> true
                ReviewFilter.WRONG -> !it.correct
                ReviewFilter.FLAGGED -> it.flagged
            }
        }
        if (shown.isEmpty()) EmptyState(Illustration.LIBRARY, "Nothing here", "No questions match this filter.")
        shown.forEach { AnswerItem(it) { onReadAbout(it.question.sectionId, it.question.id) } }
    }
}

@Composable
private fun AnswerItem(item: ReviewedAnswer, onReadAbout: () -> Unit) {
    val c = LituTheme.colors
    var expanded by rememberSaveable(item.question.id) { mutableStateOf(false) }
    LituCard(onClick = { expanded = !expanded }, modifier = Modifier.animateContentSize().semantics {
        stateDescription = (if (item.correct) "Correct" else "Wrong") + if (expanded) ", expanded" else ", collapsed"
    }) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (item.correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (item.correct) c.success else c.error)
            Text("${item.number}. ${item.question.stem}", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.weight(1f))
            if (item.flagged) Icon(Icons.Rounded.Flag, "Flagged", tint = c.accent, modifier = Modifier.size(18.dp))
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.textSecondary)
        }
        if (expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val chosen = item.question.options.filter { it.id in item.chosen }.joinToString(" and ") { it.label }
                val correct = item.question.options.filter { it.isCorrect }.joinToString(" and ") { it.label }
                Text("Your answer: ${chosen.ifEmpty { "Not answered" }}", style = LituTheme.type.body, color = if (item.correct) c.success else c.error)
                if (!item.correct) Text("Correct answer: $correct", style = LituTheme.type.body, color = c.success)
                Text(item.question.explanation, style = LituTheme.type.body, color = c.textPrimary)
                Text(item.question.handbookRef, style = LituTheme.type.caption, color = c.textSecondary)
                if (!item.correct) {
                    com.myday.litu.core.designsystem.component.LituButton(
                        "Read about this", onReadAbout,
                        variant = com.myday.litu.core.designsystem.component.ButtonVariant.TEXT, fillWidth = false,
                    )
                }
            }
        }
    }
}
