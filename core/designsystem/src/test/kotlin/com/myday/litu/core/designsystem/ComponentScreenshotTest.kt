package com.myday.litu.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.myday.litu.core.designsystem.component.AnswerCard
import com.myday.litu.core.designsystem.component.AnswerState
import com.myday.litu.core.designsystem.component.BrandCard
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.ChapterDotChip
import com.myday.litu.core.designsystem.component.ChapterTile
import com.myday.litu.core.designsystem.component.CountdownChip
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.MasteryBar
import com.myday.litu.core.designsystem.component.PassFailBadge
import com.myday.litu.core.designsystem.component.PlanCard
import com.myday.litu.core.designsystem.component.ReadinessRing
import com.myday.litu.core.designsystem.component.StreakChip
import com.myday.litu.core.designsystem.component.ThemePicker
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.model.ThemePreference
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** Key components in light, dark and at 200% font, to catch layout breaks (spec 16). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ComponentScreenshotTest(private val variant: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = listOf(arrayOf("light"), arrayOf("dark"), arrayOf("font200"))
    }

    private fun shot(name: String, content: @Composable () -> Unit) = captureRoboImage("screenshots/${name}_$variant.png") {
        val theme = if (variant == "dark") ThemePreference.DARK else ThemePreference.LIGHT
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, if (variant == "font200") 2f else 1f)) {
            LituTheme(theme) {
                Column(Modifier.background(LituTheme.colors.background).width(360.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }

    @Test fun answerCards() = shot("answer_cards") {
        AnswerState.entries.forEach { AnswerCard("Henry VIII", it, onClick = {}) }
        AnswerCard("Choose 2 variant", AnswerState.SELECTED, onClick = {}, multiSelect = true)
    }

    @Test fun buttonsAndBadges() = shot("buttons_badges") {
        LituButton("Continue studying", {})
        LituButton("Disabled", {}, enabled = false)
        LituButton("Loading", {}, loading = true)
        LituButton("Review answers", {}, variant = ButtonVariant.SECONDARY)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PassFailBadge(true)
            PassFailBadge(false)
            StreakChip(5)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownChip(Duration.ofMinutes(38))
            CountdownChip(Duration.ofMinutes(4))
        }
        ThemePicker(ThemePreference.SYSTEM, {})
    }

    @Test fun chapterAndProgress() = shot("chapters_progress") {
        (1..5).forEach { n ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChapterTile(n)
                MasteryBar(n / 5f, LituTheme.colors.chapter(n), Modifier.weight(1f))
            }
        }
        ChapterTile(3, locked = true)
        ChapterDotChip("Tudors", 3)
        PlanCard("Annual", "£34.99 / year · £2.92 / month", selected = true, onClick = {}, badge = "Best value")
        PlanCard("Monthly", "£7.99 / month", selected = false, onClick = {})
    }

    @Test fun brandCard() = shot("brand_card") {
        listOf(null, 45, 72, 88).forEach { score ->
            BrandCard { ReadinessRing(score) }
        }
    }
}
