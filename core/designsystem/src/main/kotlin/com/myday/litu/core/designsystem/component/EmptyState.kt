package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.illustration.LineIllustration
import com.myday.litu.core.designsystem.theme.LituTheme

/** Empty and error states: review empty (S13b), no mocks, billing unavailable, audio not downloaded. */
@Composable
fun EmptyState(
    illustration: Illustration,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineIllustration(illustration)
        Text(title, style = LituTheme.type.headline, color = LituTheme.colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(body, style = LituTheme.type.body, color = LituTheme.colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null) LituButton(actionLabel, onAction, Modifier.padding(top = 8.dp))
    }
}
