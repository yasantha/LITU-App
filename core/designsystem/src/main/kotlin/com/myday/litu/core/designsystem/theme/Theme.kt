package com.myday.litu.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.myday.litu.core.model.ThemePreference

/** Spacing, shape and size tokens from spec section 19.6. */
object LituDimens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val screenMargin = 16.dp
    val screenMarginTablet = 24.dp
    val minTouch = 48.dp
    val answerGap = 8.dp

    val chipShape = RoundedCornerShape(8.dp)
    val cardShape = RoundedCornerShape(12.dp)
    val brandCardShape = RoundedCornerShape(16.dp)
    val pillShape = RoundedCornerShape(28.dp)
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/** Motion tokens: 150–250 ms ease-out; answer feedback 200 ms. */
object LituMotion {
    const val SHORT_MS = 150
    const val FEEDBACK_MS = 200
    const val MEDIUM_MS = 250
}

object LituTheme {
    val colors: LituColors @Composable get() = LocalLituColors.current
    val type: LituTypography @Composable get() = LocalLituTypography.current
}

@Composable
fun isDarkTheme(preference: ThemePreference): Boolean = when (preference) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

/** Light, Dark or System (default); applies instantly without restarting (19.7). */
@Composable
fun LituTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colors = if (isDarkTheme(preference)) DarkLituColors else LightLituColors
    val type = LituTypography()
    CompositionLocalProvider(LocalLituColors provides colors, LocalLituTypography provides type) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = type.toMaterial(),
            shapes = Shapes(
                extraSmall = LituDimens.chipShape,
                small = LituDimens.chipShape,
                medium = LituDimens.cardShape,
                large = LituDimens.brandCardShape,
                extraLarge = LituDimens.pillShape,
            ),
            content = content,
        )
    }
}

private fun LituColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = textPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = textPrimary,
        tertiary = plum,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
        surfaceBright = surface,
        outline = outline,
        outlineVariant = outline,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = textPrimary,
        inverseSurface = textPrimary,
        inverseOnSurface = background,
        scrim = base.scrim,
    )
}
