package com.myday.litu.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from spec sections 19.1–19.3, named exactly as in the spec and the Figma styles.
 * Coral and amber are fills and icons only: never text on background or surface.
 */
@Immutable
data class LituColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val success: Color,
    val successContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val warning: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    // 19.2 brand colours
    val brand: Color,
    val onBrand: Color,
    val onBrandMuted: Color,
    val brandAmber: Color,
    val onBrandAmber: Color,
    val brandCoral: Color,
    val coralContainer: Color,
    val onCoralContainer: Color,
    val amberContainer: Color,
    val plum: Color,
    val plumContainer: Color,
    val onPlumContainer: Color,
    val blueContainer: Color,
    val streak: Color,
    val isDark: Boolean,
) {
    /** 19.3: one colour per handbook chapter, always shown next to the chapter number or name. */
    fun chapter(number: Int): Color = when (number) {
        1 -> plum
        2 -> primary
        3 -> brandCoral
        4 -> CHAPTER_4
        else -> secondary
    }

    fun chapterContainer(number: Int): Color = when (number) {
        1 -> plumContainer
        2 -> primaryContainer
        3 -> coralContainer
        4 -> amberContainer
        else -> blueContainer
    }

    private companion object {
        val CHAPTER_4 = Color(0xFFE9A23B)
    }
}

// Fixed in both themes: the logo, splash and brand cards keep their colours (19.7).
private val Cream = Color(0xFFFFF4E0)
private val Mint = Color(0xFFE3F0EF)
private val Amber = Color(0xFFFFC24B)
private val Ink = Color(0xFF16323A)
private val Coral = Color(0xFFF2765A)

val LightLituColors = LituColors(
    primary = Color(0xFF0F6E6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F0EF),
    secondary = Color(0xFF3D5A80),
    onSecondary = Color(0xFFFFFFFF),
    accent = Color(0xFFE9A23B),
    success = Color(0xFF1E7B4F),
    successContainer = Color(0xFFE4F2EA),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9E3E1),
    onError = Color(0xFFFFFFFF),
    warning = Color(0xFF8A5A00),
    background = Color(0xFFFBF8F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEFEC),
    textPrimary = Color(0xFF1A1F1E),
    textSecondary = Color(0xFF4F5A58),
    outline = Color(0xFFD3D6CF),
    brand = Color(0xFF0F6E6E),
    onBrand = Cream,
    onBrandMuted = Mint,
    brandAmber = Amber,
    onBrandAmber = Ink,
    brandCoral = Coral,
    coralContainer = Color(0xFFFDE7E1),
    onCoralContainer = Color(0xFFB5452C),
    amberContainer = Color(0xFFFFF1D6),
    plum = Color(0xFF6A4C9C),
    plumContainer = Color(0xFFEEE8F6),
    onPlumContainer = Color(0xFF6A4C9C),
    blueContainer = Color(0xFFE4EAF3),
    streak = Color(0xFFE07B00),
    isDark = false,
)

val DarkLituColors = LituColors(
    primary = Color(0xFF6FD3CF),
    onPrimary = Color(0xFF00302F),
    primaryContainer = Color(0xFF124F4E),
    secondary = Color(0xFFA9C3E8),
    onSecondary = Color(0xFF0F2440),
    accent = Color(0xFFF5C57A),
    success = Color(0xFF7FD8A8),
    successContainer = Color(0xFF173A2A),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF3F1F1D),
    onError = Color(0xFF601410),
    warning = Color(0xFFF5C57A),
    background = Color(0xFF101414),
    surface = Color(0xFF1A2020),
    surfaceVariant = Color(0xFF2A3331),
    textPrimary = Color(0xFFE7EDEB),
    textSecondary = Color(0xFFB5C1BE),
    outline = Color(0xFF3E4846),
    brand = Color(0xFF0F5E5E),
    onBrand = Cream,
    onBrandMuted = Mint,
    brandAmber = Amber,
    onBrandAmber = Ink,
    brandCoral = Coral,
    coralContainer = Color(0xFF3D221B),
    onCoralContainer = Color(0xFFF4A08A),
    amberContainer = Color(0xFF3A2E14),
    plum = Color(0xFFB49BE0),
    plumContainer = Color(0xFF2B2340),
    onPlumContainer = Color(0xFFC9B5EE),
    blueContainer = Color(0xFF1E2A3B),
    streak = Color(0xFFF5A04A),
    isDark = true,
)

/** Logo colours (19.4). The logo never changes colour between themes. */
object LogoColors {
    val Tile = Color(0xFF0F6E6E)
    val Cream = Color(0xFFFFF4E0)
    val Amber = Color(0xFFFFC24B)
    val Coral = Color(0xFFF2765A)
    val Ink = Color(0xFF16323A)
}

val LocalLituColors = staticCompositionLocalOf { LightLituColors }
