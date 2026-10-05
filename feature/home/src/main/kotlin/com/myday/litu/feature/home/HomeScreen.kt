package com.myday.litu.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.myday.litu.core.domain.plan.PlanStep
import com.myday.litu.core.domain.plan.StudyPlan
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myday.litu.core.designsystem.component.BrandCard
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.ChapterDotChip
import com.myday.litu.core.designsystem.component.FreezeChip
import com.myday.litu.core.designsystem.component.InfoBanner
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituCard
import com.myday.litu.core.designsystem.component.LituProgressBar
import com.myday.litu.core.designsystem.component.ModeTile
import com.myday.litu.core.designsystem.component.ReadinessRing
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.StatusChip
import com.myday.litu.core.designsystem.component.StreakChip
import com.myday.litu.core.designsystem.icon.LogoTile
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.readiness.ReadinessLevel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** S07: readiness first, then today's goal. */
@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onMockTests: () -> Unit,
    onTimer: () -> Unit,
    onReview: () -> Unit,
    onContinue: (weakestChapterIds: List<String>) -> Unit,
    onPractiseSection: (String) -> Unit,
    onLocked: () -> Unit,
    onOpenNotes: (sectionId: String) -> Unit,
    onPractiseSections: (sectionIds: List<String>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val c = LituTheme.colors
    ScreenColumn(Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoTile(44.dp)
            Column(Modifier.weight(1f)) {
                Text(s.today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)), style = LituTheme.type.caption, color = c.textSecondary)
                Text(greeting(), style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.semantics { heading() })
            }
            Surface(onClick = onSettings, shape = CircleShape, color = c.coralContainer, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Person, "Settings", tint = c.onCoralContainer, modifier = Modifier.padding(12.dp))
            }
        }
        if (s.banner.isNotBlank()) InfoBanner(s.banner, icon = Icons.Rounded.Campaign)

        ReadinessCard(s, onMockTests)
        val openStep: (PlanStep) -> Unit = { step ->
            when (step) {
                is PlanStep.Read -> onOpenNotes(step.sectionId)
                is PlanStep.Practise -> onPractiseSections(step.sectionIds)
                is PlanStep.Review -> onReview()
            }
        }
        TodayCard(s, onContinue = {
            // Continue with the next step of today's plan, otherwise practise the weakest chapters.
            val next = s.plan?.nextStep
            when {
                next != null -> openStep(next)
                s.isPro -> onContinue(s.weakestChapterIds)
                else -> onLocked()
            }
        })
        s.plan?.let { PlanCard(it, openStep) }

        LituCard(onClick = onReview) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = c.coralContainer, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Replay, null, tint = c.onCoralContainer, modifier = Modifier.padding(8.dp))
                }
                Text(
                    when (s.dueCount) { 0 -> "Nothing to review"; 1 -> "1 question to review"; else -> "${s.dueCount} questions to review" },
                    style = LituTheme.type.label, color = c.textPrimary, modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
            }
        }

        if (s.weakTopics.isNotEmpty()) {
            Text("Weakest topics", style = LituTheme.type.label, color = c.textPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.weakTopics.forEach { t ->
                    ChapterDotChip(t.title, t.chapterNumber, onClick = { if (s.isPro) onPractiseSection(t.sectionId) else onLocked() })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(c.plumContainer, onMockTests, Modifier.weight(1f).heightIn(min = 64.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LibraryBooks, null, tint = c.onPlumContainer)
                    Text("Mock test", style = LituTheme.type.label, color = c.textPrimary)
                }
            }
            ModeTile(c.amberContainer, onTimer, Modifier.weight(1f).heightIn(min = 64.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Timer, null, tint = c.warning)
                    Text("Study timer", style = LituTheme.type.label, color = c.textPrimary)
                }
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun ReadinessCard(s: HomeState, onMockTests: () -> Unit) {
    val c = LituTheme.colors
    BrandCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReadinessRing(s.readiness)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (s.readiness == null) {
                    Text("Take a mock test to get your score", style = LituTheme.type.title, color = c.onBrand)
                    Text("Your readiness uses your mock results and what you have mastered.", style = LituTheme.type.caption, color = c.onBrandMuted)
                    LituButton("Start mock test", onMockTests, variant = ButtonVariant.AMBER, fillWidth = false, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text(
                        when (s.level) {
                            ReadinessLevel.LIKELY_TO_PASS -> "Likely to pass"
                            ReadinessLevel.GETTING_CLOSE -> "Getting close"
                            else -> "Keep practising"
                        },
                        style = LituTheme.type.title, color = c.onBrand,
                    )
                    Text("Pass mark is 75%", style = LituTheme.type.caption, color = c.onBrandMuted)
                    if (s.lastMockScores.isNotEmpty()) {
                        Text("Last mocks: ${s.lastMockScores.joinToString(", ")}", style = LituTheme.type.caption, color = c.onBrandMuted)
                    }
                }
                s.daysUntilTest?.let { days ->
                    StatusChip(
                        if (days == 1) "Test tomorrow" else "Test in $days days",
                        Icons.Rounded.CalendarMonth, c.brandAmber, c.onBrandAmber,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayCard(s: HomeState, onContinue: () -> Unit) {
    val c = LituTheme.colors
    val t = s.todayProgress
    LituCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Today: ${t.answered} of ${t.goal} questions", style = LituTheme.type.label, color = c.textPrimary)
                Text(
                    if (t.remaining == 0) "You've met today's goal." else "You're ${t.remaining} questions from today's goal.",
                    style = LituTheme.type.caption, color = c.textSecondary,
                )
            }
            if (t.streak.current > 0) StreakChip(t.streak.current)
        }
        LituProgressBar(t.fraction, Modifier.padding(vertical = 12.dp))
        if (t.streak.frozenDays.isNotEmpty()) FreezeChip(Modifier.padding(bottom = 12.dp))
        LituButton("Continue studying", onContinue)
    }
}

/** Today's improvement plan for the weakest sections (read, practise, review). */
@Composable
private fun PlanCard(plan: StudyPlan, onStep: (PlanStep) -> Unit) {
    val c = LituTheme.colors
    LituCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your plan for today", style = LituTheme.type.title, color = c.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            if (!plan.complete) Text("about ${plan.minutesLeft} min", style = LituTheme.type.caption, color = c.textSecondary)
        }
        when {
            plan.testSoon -> "Your test is close, so this focuses on the chapters with the most questions."
            plan.exploring -> "Start here: a section you have not practised much yet."
            else -> "Built from the questions you found hardest."
        }.let { Text(it, style = LituTheme.type.caption, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp)) }
        plan.improvements.forEach { i ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = c.success, modifier = Modifier.size(20.dp))
                Text("${i.title}: ${i.fromPercent}% → ${i.toPercent}%. Moving on.", style = LituTheme.type.label, color = c.textPrimary)
            }
        }
        plan.steps.forEachIndexed { index, step ->
            val label = when (step) {
                is PlanStep.Read -> "Read: ${step.title}"
                is PlanStep.Practise -> "Practise: ${step.questions} questions on ${step.titles.joinToString(" and ")}"
                is PlanStep.Review -> "Review: ${step.questions} questions you missed before"
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !step.done) { onStep(step) }.padding(top = 8.dp)
                    .semantics(mergeDescendants = true) { stateDescription = if (step.done) "Done" else "${step.minutes} minutes" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step.done) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = c.success, modifier = Modifier.size(28.dp))
                } else {
                    Surface(shape = CircleShape, color = c.primaryContainer, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = LituTheme.type.label, color = c.textPrimary) }
                    }
                }
                Text(label, style = LituTheme.type.body, color = if (step.done) c.textSecondary else c.textPrimary, modifier = Modifier.weight(1f))
                Text(if (step.done) "Done" else "${step.minutes} min", style = LituTheme.type.caption, color = if (step.done) c.success else c.textSecondary)
            }
        }
        if (plan.complete) {
            Text("Plan complete. A new plan is ready tomorrow.", style = LituTheme.type.label, color = c.success, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
