package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.model.ThemePreference

enum class TopBarNav { BACK, CLOSE, NONE }

@Composable
fun LituTopBar(
    title: String,
    modifier: Modifier = Modifier,
    nav: TopBarNav = TopBarNav.BACK,
    onNav: () -> Unit = {},
    centered: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (nav) {
            TopBarNav.BACK -> IconButton(onNav) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            TopBarNav.CLOSE -> IconButton(onNav) { Icon(Icons.Rounded.Close, "Close") }
            TopBarNav.NONE -> Box(Modifier.size(12.dp))
        }
        Text(
            title,
            style = if (centered) LituTheme.type.label else LituTheme.type.title,
            color = LituTheme.colors.textPrimary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        if (nav != TopBarNav.NONE && centered) Box(Modifier.size(0.dp))
    }
}

/** Light, Dark or System segmented control for Settings › Display (spec 19.7). */
@Composable
fun ThemePicker(selected: ThemePreference, onSelect: (ThemePreference) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(ThemePreference.LIGHT to "Light", ThemePreference.DARK to "Dark", ThemePreference.SYSTEM to "System")
    val c = LituTheme.colors
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = c.primaryContainer,
                    activeContentColor = c.textPrimary,
                    inactiveContainerColor = c.surface,
                    inactiveContentColor = c.textPrimary,
                    activeBorderColor = c.primary,
                    inactiveBorderColor = c.outline,
                ),
                label = { Text(label, style = LituTheme.type.label) },
            )
        }
    }
}

enum class NavigatorState { UNANSWERED, ANSWERED, FLAGGED, CURRENT }

/** Square in the mock navigator sheet (S15b). */
@Composable
fun NavigatorSquare(number: Int, state: NavigatorState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    val (container, content, border) = when (state) {
        NavigatorState.UNANSWERED -> Triple(c.surface, c.textPrimary, BorderStroke(1.dp, c.outline))
        NavigatorState.ANSWERED -> Triple(c.primaryContainer, c.textPrimary, BorderStroke(1.dp, c.primary))
        NavigatorState.FLAGGED -> Triple(c.amberContainer, c.warning, BorderStroke(1.dp, c.accent))
        NavigatorState.CURRENT -> Triple(c.primary, c.onPrimary, BorderStroke(2.dp, c.primary))
    }
    val description = "Question $number, " + when (state) {
        NavigatorState.UNANSWERED -> "not answered"
        NavigatorState.ANSWERED -> "answered"
        NavigatorState.FLAGGED -> "flagged"
        NavigatorState.CURRENT -> "current"
    }
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = description; this.role = Role.Button },
        shape = LituDimens.chipShape,
        color = container,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$number", style = LituTheme.type.label, color = content)
            if (state == NavigatorState.FLAGGED) {
                Icon(Icons.Rounded.Flag, null, tint = c.accent, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp))
            }
        }
    }
}

/** Paywall plan card: default, selected, "Best value" badge. */
@Composable
fun PlanCard(
    title: String,
    priceLine: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val c = LituTheme.colors
    Box(modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(top = if (badge != null) 10.dp else 0.dp)
                .semantics { this.role = Role.RadioButton },
            shape = LituDimens.cardShape,
            color = if (selected) c.primaryContainer else c.surface,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.outline),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = LituTheme.type.label, color = c.textPrimary)
                    Text(priceLine, style = LituTheme.type.caption, color = c.textSecondary)
                }
                androidx.compose.material3.RadioButton(selected = selected, onClick = null)
            }
        }
        if (badge != null) {
            Surface(
                Modifier.align(Alignment.TopEnd).padding(end = 12.dp),
                shape = LituDimens.chipShape,
                color = c.primary,
            ) { Text(badge, style = LituTheme.type.caption, color = c.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
        }
    }
}

/** Small banner for offline or Remote Config messages. */
@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Wifi) {
    val c = LituTheme.colors
    Surface(modifier.fillMaxWidth(), shape = LituDimens.cardShape, color = c.amberContainer) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.warning, modifier = Modifier.size(20.dp))
            Text(text, style = LituTheme.type.body, color = c.textPrimary)
        }
    }
}
