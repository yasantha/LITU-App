package com.myday.litu.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.LegalLinks
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.icon.LogoMark
import com.myday.litu.core.designsystem.theme.LituTheme
import kotlinx.coroutines.launch

private data class Slide(val headline: String, val line: String)

private val slides = listOf(
    Slide("Pass first time", "The test is 24 questions in 45 minutes. You need 18 right to pass."),
    Slide("Study in short bursts", "Ten minutes on the bus is enough. Questions you miss come back until you know them."),
    Slide("Know when you're ready", "Mock tests and your readiness score show when you are likely to pass."),
)

/** S02: three slides on a brand hero with the clock tower scene. */
@Composable
internal fun WelcomeScreen(onContinue: () -> Unit) {
    val c = LituTheme.colors
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(c.background)) {
        Box(
            Modifier.fillMaxWidth().weight(0.45f).background(c.brand).statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            LogoMark(160.dp)
        }
        HorizontalPager(pager, Modifier.weight(0.35f)) { page ->
            Column(
                Modifier.fillMaxSize().padding(24.dp).widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(slides[page].headline, style = LituTheme.type.headline, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                Text(slides[page].line, style = LituTheme.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
            }
        }
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Page ${pager.currentPage + 1} of ${slides.size}" },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(slides.size) { i ->
                Box(Modifier.size(if (i == pager.currentPage) 10.dp else 8.dp).clip(CircleShape).background(if (i == pager.currentPage) c.primary else c.outline))
            }
        }
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LituButton("Continue", {
                if (pager.currentPage < slides.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onContinue()
            })
            LituButton("Skip", onContinue, variant = ButtonVariant.TEXT)
            Spacer(Modifier.height(4.dp))
            Text(LegalLinks.DISCLAIMER, style = LituTheme.type.caption, color = c.textSecondary, textAlign = TextAlign.Center)
        }
    }
}
