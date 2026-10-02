package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.icon.LituLogo
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme

/** Flat card with a 1 dp outline (elevation is only for sheets and dialogs). */
@Composable
fun LituCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = LituTheme.colors.surface,
    border: Boolean = true,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val stroke = if (border) BorderStroke(1.dp, LituTheme.colors.outline) else null
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = LituDimens.cardShape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = LituDimens.cardShape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Brand card on `brand` with a faint 16% tower decoration: readiness card and new-mock card.
 * Keeps its colours in both themes.
 */
@Composable
fun BrandCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(LituDimens.brandCardShape)
            .background(LituTheme.colors.brand),
    ) {
        Image(
            LituLogo.monochrome(LituTheme.colors.onBrand),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 20.dp)
                .size(120.dp)
                .alpha(0.16f),
        )
        Box(Modifier.padding(20.dp), content = content)
    }
}

/** A mode tile such as Review (coral) or Mixed (amber), 16 dp corners. */
@Composable
fun ModeTile(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier, shape = LituDimens.brandCardShape, color = color) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Skeleton placeholder for loading states such as plan cards. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(LituDimens.cardShape)
            .background(LituTheme.colors.surfaceVariant)
            .border(1.dp, LituTheme.colors.outline, LituDimens.cardShape),
    )
}
