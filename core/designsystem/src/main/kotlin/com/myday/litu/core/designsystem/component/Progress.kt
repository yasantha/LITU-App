package com.myday.litu.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituMotion
import com.myday.litu.core.designsystem.theme.LituTheme

/** Rounded linear progress, e.g. question progress and today's goal. */
@Composable
fun LituProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = LituTheme.colors.primary,
    track: Color = LituTheme.colors.surfaceVariant,
    height: Dp = 8.dp,
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(LituMotion.MEDIUM_MS), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(track),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(height)).background(color))
    }
}

/** Mastery bar in the chapter colour: 0%, partial, 100% with a mastered tick. */
@Composable
fun MasteryBar(fraction: Float, color: Color, modifier: Modifier = Modifier, showPercent: Boolean = true) {
    val percent = (fraction * 100).toInt()
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$percent% mastered" }, verticalAlignment = Alignment.CenterVertically) {
        LituProgressBar(fraction, Modifier.weight(1f), color = color, height = 6.dp)
        if (showPercent) {
            Box(Modifier.widthIn(min = 48.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterEnd) {
                if (percent >= 100) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Mastered", tint = LituTheme.colors.success, modifier = Modifier.size(18.dp))
                } else {
                    Text("$percent%", style = LituTheme.type.label, color = LituTheme.colors.textPrimary, softWrap = false)
                }
            }
        }
    }
}

/**
 * Readiness ring on the brand card (amber on teal). States: no mock yet, below 60%, 60–79%, 80%+.
 * The label beside the ring carries the level in words.
 */
@Composable
fun ReadinessRing(score: Int?, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val c = LituTheme.colors
    val sweep by animateFloatAsState((score ?: 0) / 100f, tween(600), label = "readiness")
    Box(
        modifier.size(size).clearAndSetSemantics {
            contentDescription = if (score == null) "No readiness score yet" else "Readiness $score percent"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val w = this.size.minDimension * 0.1f
            drawArc(c.onBrand.copy(alpha = 0.18f), -90f, 360f, false, style = Stroke(w))
            if (score != null) drawArc(c.brandAmber, -90f, 360f * sweep, false, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (score == null) "–" else "$score%", style = LituTheme.type.display.copy(fontSize = LituTheme.type.title.fontSize * 1.3f), color = c.onBrand)
        }
    }
}

/** Circular timer from Daily Focus, recoloured to the design tokens. */
@Composable
fun TimerRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 240.dp, content: @Composable () -> Unit) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(400), label = "timerProgress")
    val track = LituTheme.colors.surfaceVariant
    val color = LituTheme.colors.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.minDimension * 0.075f
            drawArc(track, -90f, 360f, false, style = Stroke(w, cap = StrokeCap.Round))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}
