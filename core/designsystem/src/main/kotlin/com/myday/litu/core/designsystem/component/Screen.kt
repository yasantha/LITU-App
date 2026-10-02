package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituTheme

/**
 * Scrolling screen body on `background` with the 16 dp margin. Content is capped at 600 dp wide so
 * tablets get a readable column with wider margins.
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    spacing: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(LituTheme.colors.background)) {
        topBar()
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )
        }
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().align(Alignment.CenterHorizontally).navigationBarsPadding(),
            content = bottomBar,
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = LituTheme.type.title, color = LituTheme.colors.textPrimary, modifier = modifier.semantics { heading() })
}

/** Content is local, so loading is near-instant; this only covers the first frame. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(LituTheme.colors.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LituTheme.colors.primary)
    }
}
