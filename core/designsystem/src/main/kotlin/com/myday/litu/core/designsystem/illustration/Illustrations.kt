package com.myday.litu.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituTheme

/**
 * Simple line drawings of everyday UK life in teal with amber details (spec 18). Used on
 * onboarding, empty states and dialogs only. Decorative: callers pass no content description.
 */
enum class Illustration { STUDY_CORNER, BUS_STOP, LIBRARY }

@Composable
fun LineIllustration(kind: Illustration, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val line = LituTheme.colors.primary
    val detail = LituTheme.colors.accent
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        val stroke = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        when (kind) {
            Illustration.STUDY_CORNER -> studyCorner(::p, stroke, line, detail, s)
            Illustration.BUS_STOP -> busStop(::p, stroke, line, detail, s)
            Illustration.LIBRARY -> library(::p, stroke, line, detail, s)
        }
    }
}

private fun DrawScope.studyCorner(p: (Float, Float) -> Offset, stroke: Stroke, line: Color, detail: Color, s: Float) {
    // Desk
    drawLine(line, p(12f, 70f), p(88f, 70f), stroke.width, StrokeCap.Round)
    drawLine(line, p(18f, 70f), p(18f, 92f), stroke.width, StrokeCap.Round)
    drawLine(line, p(82f, 70f), p(82f, 92f), stroke.width, StrokeCap.Round)
    // Lamp
    drawLine(line, p(72f, 70f), p(72f, 40f), stroke.width, StrokeCap.Round)
    drawLine(line, p(72f, 40f), p(60f, 30f), stroke.width, StrokeCap.Round)
    drawPath(Path().apply { moveTo(50f * s, 36f * s); lineTo(60f * s, 24f * s); lineTo(68f * s, 34f * s); close() }, detail)
    drawCircle(detail.copy(alpha = 0.25f), 14f * s, p(52f, 52f))
    // Open book
    drawPath(Path().apply {
        moveTo(50f * s, 66f * s); quadraticTo(40f * s, 58f * s, 28f * s, 60f * s); lineTo(28f * s, 68f * s)
        quadraticTo(40f * s, 66f * s, 50f * s, 70f * s); quadraticTo(60f * s, 66f * s, 72f * s, 68f * s)
        lineTo(72f * s, 60f * s); quadraticTo(60f * s, 58f * s, 50f * s, 66f * s)
    }, line, style = stroke)
    // Mug
    drawRect(line, p(16f, 56f), Size(9f * s, 14f * s), style = stroke)
    drawArc(detail, -90f, 180f, false, p(22f, 59f), Size(7f * s, 8f * s), style = stroke)
}

private fun DrawScope.busStop(p: (Float, Float) -> Offset, stroke: Stroke, line: Color, detail: Color, s: Float) {
    // Ground
    drawLine(line, p(6f, 90f), p(94f, 90f), stroke.width, StrokeCap.Round)
    // Shelter
    drawLine(line, p(14f, 90f), p(14f, 36f), stroke.width, StrokeCap.Round)
    drawLine(line, p(48f, 90f), p(48f, 36f), stroke.width, StrokeCap.Round)
    drawLine(line, p(10f, 36f), p(52f, 32f), stroke.width, StrokeCap.Round)
    drawLine(line, p(18f, 70f), p(44f, 70f), stroke.width, StrokeCap.Round)
    // Stop sign
    drawLine(line, p(70f, 90f), p(70f, 30f), stroke.width, StrokeCap.Round)
    drawCircle(detail, 9f * s, p(70f, 26f))
    drawCircle(line, 9f * s, p(70f, 26f), style = stroke)
    // Timetable
    drawRect(detail.copy(alpha = 0.3f), p(66f, 46f), Size(8f * s, 12f * s))
    drawRect(line, p(66f, 46f), Size(8f * s, 12f * s), style = stroke)
}

private fun DrawScope.library(p: (Float, Float) -> Offset, stroke: Stroke, line: Color, detail: Color, s: Float) {
    drawRect(line, p(14f, 14f), Size(72f * s, 78f * s), style = stroke)
    for (y in listOf(38f, 62f)) drawLine(line, p(14f, y), p(86f, y), stroke.width, StrokeCap.Round)
    val books = listOf(18f to 10f, 30f to 6f, 38f to 9f, 50f to 7f, 60f to 10f, 72f to 8f)
    for ((row, top) in listOf(38f to 18f, 62f to 42f, 86f to 66f)) {
        books.forEachIndexed { i, (x, w) ->
            val shift = if (row == 62f) 4f else 0f
            val color = if ((i + row.toInt()) % 3 == 0) detail else line
            drawRect(color, p(x + shift, top + (i % 2) * 2f), Size(w * 0.7f * s, (row - top - (i % 2) * 2f) * s), style = stroke)
        }
    }
}
