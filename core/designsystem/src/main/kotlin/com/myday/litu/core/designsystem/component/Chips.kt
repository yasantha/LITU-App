package com.myday.litu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myday.litu.core.designsystem.theme.LituDimens
import com.myday.litu.core.designsystem.theme.LituTheme
import java.time.Duration

@Composable
fun LituFilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = LituTheme.type.label) },
        modifier = modifier.height(40.dp),
        shape = LituDimens.chipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = c.surface,
            labelColor = c.textPrimary,
            selectedContainerColor = c.primaryContainer,
            selectedLabelColor = c.textPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(true, selected, borderColor = c.outline, selectedBorderColor = c.primary),
    )
}

/** Weakest-topic chip: a dot in the chapter colour beside the topic name. */
@Composable
fun ChapterDotChip(label: String, chapterNumber: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = LituTheme.colors
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.chapter(chapterNumber)))
            Text(label, style = LituTheme.type.label, color = c.textPrimary)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = LituDimens.chipShape, color = c.chapterContainer(chapterNumber), content = content)
    } else {
        Surface(modifier = modifier, shape = LituDimens.chipShape, color = c.chapterContainer(chapterNumber), content = content)
    }
}

/** Status chip with an icon and text, e.g. the streak chip (amberContainer, text in warning). */
@Composable
fun StatusChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    iconTint: Color = content,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(LituDimens.chipShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Text(text, style = LituTheme.type.label, color = content)
    }
}

@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    StatusChip(
        text = if (days == 1) "1 day" else "$days days",
        icon = Icons.Rounded.LocalFireDepartment,
        container = c.amberContainer,
        content = c.warning,
        iconTint = c.streak,
        modifier = modifier.semantics { contentDescription = "Streak: $days days" },
    )
}

/** Snowflake chip shown when a weekly freeze kept the streak going (spec 9.4). */
@Composable
fun FreezeChip(modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    StatusChip("Freeze used", Icons.Rounded.AcUnit, c.blueContainer, c.secondary, modifier = modifier)
}

/** Pass and Fail badges: icon plus text, never colour alone. */
@Composable
fun PassFailBadge(passed: Boolean, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (passed) c.successContainer else c.errorContainer)
            .border(1.dp, if (passed) c.success else c.error, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(if (passed) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (passed) c.success else c.error, modifier = Modifier.size(16.dp))
        Text(if (passed) "Passed" else "Not passed", style = LituTheme.type.label, color = if (passed) c.success else c.error)
    }
}

/** Mock countdown: normal, under 5 minutes (warning colour), expired. */
@Composable
fun CountdownChip(remaining: Duration, modifier: Modifier = Modifier) {
    val c = LituTheme.colors
    val warning = remaining <= Duration.ofMinutes(5)
    val mins = remaining.toMinutes()
    val secs = remaining.seconds % 60
    val text = "%02d:%02d".format(mins, secs)
    StatusChip(
        text = text,
        icon = Icons.Rounded.Timer,
        container = if (warning) c.amberContainer else c.surfaceVariant,
        content = if (warning) c.warning else c.textPrimary,
        modifier = modifier.clearAndSetSemantics {
            // TalkBack reads the remaining minutes; the live announcement every 5 minutes is in the mock screen.
            contentDescription = "Time left: $mins minutes"
        },
    )
}

/** Chapter number tile: chapter container fill with a 4 dp base line in the chapter colour. */
@Composable
fun ChapterTile(number: Int, modifier: Modifier = Modifier, locked: Boolean = false, size: Dp = 40.dp) {
    val c = LituTheme.colors
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(c.chapterContainer(number))
            .semantics { contentDescription = "Chapter $number" + if (locked) ", locked" else "" },
    ) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(c.chapter(number)))
        if (locked) {
            Icon(Icons.Rounded.Lock, null, tint = c.textSecondary, modifier = Modifier.align(Alignment.Center).size(18.dp))
        } else {
            Text(
                "$number",
                style = LituTheme.type.title.copy(fontWeight = FontWeight.Bold),
                color = c.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun LockBadge(modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = LituTheme.colors.textSecondary, modifier = modifier.size(18.dp))
}
