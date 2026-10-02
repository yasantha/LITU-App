package com.myday.litu.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myday.litu.core.designsystem.R

// Both fonts are bundled (OFL), never downloaded. Atkinson Hyperlegible was designed for letter
// clarity, which helps readers whose first language is not English.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

/** Type scale from spec section 19.5. */
@Immutable
data class LituTypography(
    val display: TextStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    val headline: TextStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    val title: TextStyle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    val question: TextStyle = TextStyle(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 30.sp),
    val body: TextStyle = TextStyle(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    val label: TextStyle = TextStyle(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    val caption: TextStyle = TextStyle(fontFamily = AtkinsonHyperlegible, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
)

internal fun LituTypography.toMaterial() = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = headline,
    titleLarge = title,
    titleMedium = title.copy(fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = caption,
    labelLarge = label,
    labelMedium = label.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = caption,
)

val LocalLituTypography = staticCompositionLocalOf { LituTypography() }
