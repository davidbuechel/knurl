package de.knurl.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.knurl.ui.theme.Knurl

/*
 * Knurl's building blocks. Rules: surfaces carry no border, only fill and space; no card inside a
 * card; one accent (primary); green only for "done", red only for records.
 */

/** A rounded surface that groups rows, separated by hairlines (like system settings). */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content,
    )
}

/**
 * One row in a [ListGroup]. [leading] is usually an icon tile or a thumbnail; [trailing] a value,
 * chip or switch. With [onClick] and no trailing, a chevron shows.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
        if (divider) HorizontalDivider(Modifier.padding(start = if (leading != null) 66.dp else 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/** A [ListRow] with a switch; the whole row toggles. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
) {
    ListRow(
        title, subtitle = subtitle, leading = leading, divider = divider,
        trailing = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        onClick = if (enabled) ({ onChange(!checked) }) else null,
    )
}

/** A [ListRow] that shows its current value and opens an editor on tap. */
@Composable
fun ValueRow(title: String, value: String, onClick: () -> Unit, subtitle: String? = null, divider: Boolean = true) {
    ListRow(
        title, subtitle = subtitle, divider = divider, onClick = onClick,
        trailing = {
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        },
    )
}

/** A title with a [SegmentedTabs] switch below, for two or three choices inside a [ListGroup]. */
@Composable
fun <T> SegmentedRow(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, divider: Boolean = true) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            SegmentedTabs(options, selected, label, onSelect)
        }
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/**
 * For long lazy lists that should look like one [ListGroup]: each item gets the group's fill, and
 * the first and last item its rounded corners.
 */
@Composable
fun Modifier.groupedItem(index: Int, count: Int): Modifier {
    val r = 22.dp // MaterialTheme.shapes.large, as in ListGroup
    val shape: Shape = RoundedCornerShape(
        topStart = if (index == 0) r else 0.dp, topEnd = if (index == 0) r else 0.dp,
        bottomStart = if (index == count - 1) r else 0.dp, bottomEnd = if (index == count - 1) r else 0.dp,
    )
    return fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLowest)
}

/** A coloured square with an icon, the leading element of settings-style rows. */
@Composable
fun IconTile(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(Modifier.size(36.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Section title above a group, with an optional action on the right ("All"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
        }
    }
}

/** A key number; [change] is whole percent against the period before (null: nothing to compare). */
data class Kpi(val value: String, val label: String, val change: Int? = null)

/** Three key numbers side by side, each on its own quiet tile. */
@Composable
fun KpiRow(items: List<Kpi>, modifier: Modifier = Modifier, leading: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        leading?.invoke(this)
        items.forEach { kpi ->
            Column(
                Modifier.weight(1f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(kpi.value, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Two lines rather than cut off ("4 / weeks in a row").
                Text(kpi.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                kpi.change?.let { change ->
                    // Green only for "more"; less is grey, not red: a lighter week is not a failure.
                    Text(
                        when { change > 0 -> "▲ " + Formatting.percent(change); change < 0 -> "▼ " + Formatting.percent(-change); else -> "± " + Formatting.percent(0) },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (change > 0) Knurl.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Two or three views of one screen (Routines / History), as a pill switch. */
@Composable
fun <T> SegmentedTabs(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(3.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (on) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The one big call to action on a screen: cobalt gradient, white button. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)))
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(brush).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Monday to Sunday as dots: filled for a training day, ringed for today. */
@Composable
fun WeekDots(labels: List<String>, trained: List<Boolean>, todayIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                val today = i == todayIndex
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(if (trained[i]) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .then(if (today && !trained[i]) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier),
                )
            }
        }
    }
}

/** A small pill label: "PR", "Next", "Superset A". */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
