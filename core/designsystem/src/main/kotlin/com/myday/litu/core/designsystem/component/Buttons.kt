package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme

enum class ButtonVariant { PRIMARY, SECONDARY, TEXT, AMBER }

/**
 * Pill button (28 dp corners). States: default, pressed, disabled, loading. The amber variant is for
 * brand cards. Text wraps rather than truncating at 200% font size.
 */
@Composable
fun LituButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fillWidth: Boolean = true,
) {
    val colors = LituTheme.colors
    val m = (if (fillWidth) modifier.fillMaxWidth() else modifier).defaultMinSize(minHeight = 52.dp)
    val padding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = when (variant) {
                    ButtonVariant.PRIMARY -> colors.onPrimary
                    ButtonVariant.AMBER -> colors.onBrandAmber
                    else -> colors.primary
                })
            } else if (icon != null) {
                Icon(icon, contentDescription = null, Modifier.size(20.dp))
            }
            Text(text, style = LituTheme.type.label, textAlign = TextAlign.Center)
        }
    }
    val active = enabled && !loading
    when (variant) {
        ButtonVariant.PRIMARY -> Button(
            onClick, m, active, shape = LituDimens.pillShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceVariant,
                disabledContentColor = colors.textSecondary,
            ),
        ) { content() }
        ButtonVariant.AMBER -> Button(
            onClick, m, active, shape = LituDimens.pillShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandAmber,
                contentColor = colors.onBrandAmber,
                disabledContainerColor = colors.brandAmber.copy(alpha = 0.5f),
                disabledContentColor = colors.onBrandAmber,
            ),
        ) { content() }
        ButtonVariant.SECONDARY -> OutlinedButton(
            onClick, m, active, shape = LituDimens.pillShape, contentPadding = padding,
            border = BorderStroke(1.dp, if (active) colors.primary else colors.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
        ) { content() }
        ButtonVariant.TEXT -> TextButton(
            onClick, (if (fillWidth) modifier.fillMaxWidth() else modifier).defaultMinSize(minHeight = LituDimens.minTouch), active, shape = LituDimens.pillShape,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
        ) { content() }
    }
}
