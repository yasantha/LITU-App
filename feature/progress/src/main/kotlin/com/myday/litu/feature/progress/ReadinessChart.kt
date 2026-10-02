package com.myday.litu.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.usecase.ReadinessPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Readiness over time: one series (no legend; the card title names it), 2 dp line, 8 dp markers,
 * a recessive grid and a dashed 75% pass line labelled in text ink. Tap a point to see its value.
 * TalkBack reads the whole series as text.
 */
@Composable
internal fun ReadinessChart(points: List<ReadinessPoint>, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    val measurer = rememberTextMeasurer()
    val caption = LituTheme.type.caption.copy(color = c.textSecondary)
    val valueStyle = LituTheme.type.label.copy(color = c.textPrimary)
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    var selected by remember(points) { mutableStateOf(points.lastIndex) }
    val description = "Readiness over time. " + points.joinToString("; ") {
        "${it.at.atZone(ZoneId.systemDefault()).format(fmt)}: ${it.score}%"
    } + ". Pass line 75%."

    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(180.dp).clearAndSetSemantics { contentDescription = description },
        ) {
            Canvas(
                Modifier.fillMaxWidth().height(180.dp).pointerInput(points) {
                    detectTapGestures { tap ->
                        val left = 32.dp.toPx()
                        val w = size.width - left - 8.dp.toPx()
                        if (points.size > 1) {
                            selected = points.indices.minBy { i -> abs(left + w * i / (points.size - 1) - tap.x) }
                        }
                    }
                },
            ) {
                val left = 32.dp.toPx()
                val top = 20.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val w = size.width - left - 8.dp.toPx()
                fun y(v: Int) = bottom - (bottom - top) * v / 100f
                fun x(i: Int) = if (points.size == 1) left + w / 2 else left + w * i / (points.size - 1)

                for (v in listOf(0, 50, 100)) {
                    drawLine(c.outline, Offset(left, y(v)), Offset(left + w, y(v)), 1.dp.toPx())
                    val t = measurer.measure("$v%", caption)
                    drawText(t, topLeft = Offset(0f, y(v) - t.size.height / 2))
                }
                drawLine(
                    c.textSecondary, Offset(left, y(75)), Offset(left + w, y(75)), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
                val passLabel = measurer.measure("Pass 75%", caption)
                drawText(passLabel, topLeft = Offset(left + w - passLabel.size.width, y(75) - passLabel.size.height - 2.dp.toPx()))

                if (points.size > 1) {
                    val path = Path().apply {
                        points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.score)) else lineTo(x(i), y(p.score)) }
                    }
                    drawPath(path, c.primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
                points.forEachIndexed { i, p ->
                    // 2 dp surface ring keeps markers distinct from the line.
                    drawCircle(c.surface, 6.dp.toPx(), Offset(x(i), y(p.score)))
                    drawCircle(c.primary, 4.dp.toPx(), Offset(x(i), y(p.score)))
                }
                points.getOrNull(selected)?.let { p ->
                    val t = measurer.measure("${p.score}%", valueStyle)
                    val cx = (x(selected) - t.size.width / 2).coerceIn(left, left + w - t.size.width)
                    drawText(t, topLeft = Offset(cx, (y(p.score) - t.size.height - 8.dp.toPx()).coerceAtLeast(0f)))
                }
            }
        }
        points.getOrNull(selected)?.let {
            Text(
                "Mock on ${it.at.atZone(ZoneId.systemDefault()).format(fmt)}: readiness ${it.score}%",
                style = LituTheme.type.caption, color = c.textSecondary,
            )
        }
    }
}
