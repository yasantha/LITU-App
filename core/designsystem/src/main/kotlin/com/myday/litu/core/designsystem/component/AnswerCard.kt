package com.myday.litu.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituMotion
import com.myday.litu.core.designsystem.theme.LituTheme

/** Answer card states (spec 23.1). Right and wrong always carry an icon and a text label. */
enum class AnswerState { DEFAULT, SELECTED, CORRECT, WRONG, CORRECT_NOT_CHOSEN, DISABLED }

@Composable
fun AnswerCard(
    label: String,
    state: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
) {
    val c = LituTheme.colors
    val container by animateColorAsState(
        when (state) {
            AnswerState.SELECTED -> c.primaryContainer
            AnswerState.CORRECT, AnswerState.CORRECT_NOT_CHOSEN -> c.successContainer
            AnswerState.WRONG -> c.errorContainer
            else -> c.surfaceVariant
        },
        tween(LituMotion.FEEDBACK_MS),
        label = "answerContainer",
    )
    val border = when (state) {
        AnswerState.SELECTED -> BorderStroke(2.dp, c.primary)
        AnswerState.CORRECT, AnswerState.CORRECT_NOT_CHOSEN -> BorderStroke(2.dp, c.success)
        AnswerState.WRONG -> BorderStroke(2.dp, c.error)
        else -> BorderStroke(1.dp, c.outline)
    }
    val status = when (state) {
        AnswerState.CORRECT -> "Correct"
        AnswerState.CORRECT_NOT_CHOSEN -> "Correct answer"
        AnswerState.WRONG -> "Your answer"
        else -> null
    }
    val interactive = state == AnswerState.DEFAULT || state == AnswerState.SELECTED
    val selected = state == AnswerState.SELECTED || state == AnswerState.CORRECT || state == AnswerState.WRONG
    val selectMod = if (multiSelect) {
        Modifier.toggleable(selected, enabled = interactive, role = Role.Checkbox) { onClick() }
    } else {
        Modifier.selectable(selected, enabled = interactive, role = Role.RadioButton, onClick = onClick)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .semantics { status?.let { stateDescription = it } }
            .then(selectMod),
        shape = LituDimens.cardShape,
        color = container,
        border = border,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // At large font sizes the status label moves under the answer so neither is squeezed.
            val stacked = LocalDensity.current.fontScale > 1.3f
            val good = state != AnswerState.WRONG
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = LituTheme.type.body,
                    color = if (state == AnswerState.DISABLED) c.textSecondary else c.textPrimary,
                )
                if (stacked && status != null) Text(status, style = LituTheme.type.label, color = if (good) c.success else c.error)
            }
            when (state) {
                AnswerState.CORRECT, AnswerState.CORRECT_NOT_CHOSEN, AnswerState.WRONG -> {
                    if (!stacked) Text(status!!, style = LituTheme.type.label, color = if (good) c.success else c.error)
                    Icon(
                        if (good) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (good) c.success else c.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
                AnswerState.DISABLED -> Unit
                else -> Icon(
                    when {
                        multiSelect && selected -> Icons.Rounded.CheckBox
                        multiSelect -> Icons.Rounded.CheckBoxOutlineBlank
                        selected -> Icons.Rounded.RadioButtonChecked
                        else -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (selected) c.primary else c.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
