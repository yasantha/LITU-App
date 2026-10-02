package com.myday.litu.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.FreezeChip
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituProgressBar
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SectionHeader
import com.myday.litu.core.designsystem.component.StreakChip
import com.myday.litu.core.designsystem.component.TopBarNav
import com.myday.litu.core.designsystem.theme.LituTheme

/** S12: session summary. Encouraging and direct, never "Great job, superstar!". */
@Composable
internal fun SessionSummary(s: SessionUiState.Summary, onDone: () -> Unit, onPractiseAgain: () -> Unit) {
    val c = LituTheme.colors
    BackHandler(onBack = onDone)
    val message = when {
        s.answered == 0 -> "No questions answered this time."
        s.correct == s.answered -> "Every answer right. Keep this up."
        s.correct * 100 / s.answered >= 75 -> "Above the pass mark. Review the ones you missed."
        else -> "The questions you missed will come back in your review queue."
    }
    ScreenColumn(
        topBar = { LituTopBar("Session complete", nav = TopBarNav.CLOSE, onNav = onDone) },
        bottomBar = {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LituButton("Practise again", onPractiseAgain, variant = ButtonVariant.SECONDARY)
                LituButton("Done", onDone)
            }
        },
    ) {
        Text("${s.correct} of ${s.answered}", style = LituTheme.type.display, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        Text(message, style = LituTheme.type.body, color = c.textSecondary)
        LituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today: ${s.todayAnswered} of ${s.goal} questions", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.weight(1f))
                if (s.streak.current > 0) StreakChip(s.streak.current)
            }
            LituProgressBar(s.todayAnswered.toFloat() / s.goal, Modifier.padding(top = 8.dp))
            if (s.streak.frozenDays.isNotEmpty()) FreezeChip(Modifier.padding(top = 8.dp))
        }
        if (s.missed.isNotEmpty()) {
            SectionHeader("Questions to look at again")
            LituCard {
                s.missed.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.outline)
                    Text(m.stem, style = LituTheme.type.body, color = c.textPrimary)
                    Text("Answer: ${m.correctAnswer}", style = LituTheme.type.label, color = c.success)
                }
            }
        }
    }
}

/** S05b: sample results, always followed by the paywall. */
@Composable
internal fun SampleResults(s: SessionUiState.Summary, onSeePlans: () -> Unit) {
    val c = LituTheme.colors
    BackHandler(onBack = onSeePlans)
    ScreenColumn(
        topBar = { LituTopBar("Your free sample", nav = TopBarNav.NONE) },
        bottomBar = { LituButton("See plans", onSeePlans, Modifier.padding(16.dp)) },
    ) {
        Text("${s.correct} / ${s.answered}", style = LituTheme.type.display, color = c.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() })
        Text(
            s.weakestChapter?.let { "Start with $it: it was your weakest area." }
                ?: "You got every sample question right. Keep going to stay ready.",
            style = LituTheme.type.body, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        LituCard {
            Text("By chapter", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
            s.chapterResults.forEach { r ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.title, style = LituTheme.type.body, color = c.textPrimary, modifier = Modifier.width(130.dp))
                    MasteryBar(r.score.correct.toFloat() / r.score.total, c.chapter(r.number), Modifier.weight(1f), showPercent = false)
                    Text("${r.score.correct}/${r.score.total}", style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        LituCard(color = c.amberContainer, border = false) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lightbulb, null, tint = c.warning)
                Text("The real test has 24 questions and you need 18 to pass. Short daily practice is the surest way there.",
                    style = LituTheme.type.body, color = c.textPrimary)
            }
        }
    }
}
