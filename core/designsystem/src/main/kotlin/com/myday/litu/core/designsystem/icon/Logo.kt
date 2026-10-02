package com.myday.litu.core.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LogoColors

/**
 * The logo (spec 19.4): a generic clock tower with a coral spire and amber clock face, rising from
 * an open book with one amber and one coral page. Never recoloured between themes.
 */
object LituLogo {
    private val WindowLine = Color(0xFF9DBDB6)

    val Mark: ImageVector by lazy { build(monochrome = null) }

    fun monochrome(color: Color): ImageVector = build(color)

    private fun build(monochrome: Color?): ImageVector {
        fun c(color: Color) = SolidColor(monochrome ?: color)
        return ImageVector.Builder("LituLogo", 48.dp, 48.dp, 48f, 48f).apply {
            // Spire
            path(fill = c(LogoColors.Coral)) { moveTo(17.5f, 12.5f); lineTo(24f, 3f); lineTo(30.5f, 12.5f); close() }
            // Clock housing and shaft
            path(fill = c(LogoColors.Cream)) { roundRect(17.5f, 12f, 30.5f, 21.5f, 1f) }
            path(fill = c(LogoColors.Cream)) { roundRect(19.5f, 21f, 28.5f, 38f, 0.5f) }
            if (monochrome == null) {
                path(fill = c(LogoColors.Amber)) { circle(24f, 16.75f, 3.3f) }
                path(stroke = c(LogoColors.Ink), strokeLineWidth = 0.9f, strokeLineCap = StrokeCap.Round) {
                    moveTo(24f, 16.75f); lineTo(24f, 14.4f)
                    moveTo(24f, 16.75f); lineTo(25.7f, 17.6f)
                }
                path(stroke = SolidColor(WindowLine), strokeLineWidth = 0.9f, strokeLineCap = StrokeCap.Round) {
                    for (x in listOf(22f, 24f, 26f)) { moveTo(x, 24.5f); lineTo(x, 34.5f) }
                }
            }
            // Open book: amber left page, coral right page
            path(fill = c(LogoColors.Amber)) {
                moveTo(24f, 37.5f); curveTo(19f, 34f, 11.5f, 33.5f, 5.5f, 35.5f)
                lineTo(5.5f, 41.5f); curveTo(11.5f, 39.8f, 19f, 40.2f, 24f, 43f); close()
            }
            path(fill = c(LogoColors.Coral)) {
                moveTo(24f, 37.5f); curveTo(29f, 34f, 36.5f, 33.5f, 42.5f, 35.5f)
                lineTo(42.5f, 41.5f); curveTo(36.5f, 39.8f, 29f, 40.2f, 24f, 43f); close()
            }
        }.build()
    }

    private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        moveTo(l + rad, t); lineTo(r - rad, t); arcTo(rad, rad, 0f, false, true, r, t + rad)
        lineTo(r, b - rad); arcTo(rad, rad, 0f, false, true, r - rad, b)
        lineTo(l + rad, b); arcTo(rad, rad, 0f, false, true, l, b - rad)
        lineTo(l, t + rad); arcTo(rad, rad, 0f, false, true, l + rad, t); close()
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy); arcTo(r, r, 0f, true, true, cx + r, cy); arcTo(r, r, 0f, true, true, cx - r, cy); close()
    }
}

/** The logo without a tile, e.g. 200 dp on the splash screen. */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(LituLogo.Mark, contentDescription, modifier.size(size))
}

/** The logo on its deep teal tile: Home header (44 dp), Paywall (56 dp), Welcome hero. */
@Composable
fun LogoTile(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(LogoColors.Tile),
        contentAlignment = Alignment.Center,
    ) {
        Image(LituLogo.Mark, contentDescription, Modifier.fillMaxSize().padding(size * 0.1f))
    }
}
