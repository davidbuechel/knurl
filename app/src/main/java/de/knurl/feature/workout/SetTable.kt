package de.knurl.feature.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.knurl.R
import de.knurl.domain.model.SetKind
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.feature.common.displayName
import de.knurl.feature.common.weightText
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.theme.Knurl

/**
 * One exercise in the running workout: header with photo and last time, then a compact table
 * (set | last | weight | reps | done). The exercise with the focus gets a cobalt outline.
 */
@Composable
fun ExerciseBlock(
    card: ExerciseCard,
    index: Int,
    focus: Focus?,
    logRir: Boolean,
    onOpenExercise: () -> Unit,
    onActions: () -> Unit,
    onCell: (set: Int, field: Field) -> Unit,
    onToggle: (set: Int) -> Unit,
    onCycleKind: (set: Int) -> Unit,
    onRir: (set: Int) -> Unit,
    onAddSet: () -> Unit,
    onNote: () -> Unit,
) {
    val current = focus?.ref?.exercise == index
    val outline by animateColorAsState(if (current) MaterialTheme.colorScheme.primary else Color.Transparent, label = "outline")
    val unit = LocalWeightUnit.current
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(2.dp, outline, MaterialTheme.shapes.large)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.clickable(onClick = onOpenExercise)) {
                card.exercise?.let { ExerciseThumbnail(it, Modifier.size(48.dp)) }
            }
            Column(Modifier.weight(1f).clickable(onClick = onOpenExercise)) {
                Text(card.exercise?.displayName() ?: card.exerciseId, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val hint = card.suggestion?.takeIf { it.increased }?.let { stringResource(R.string.workout_suggest_short, weightText(it.weightKg)) }
                    ?: card.lastTime.takeIf { it.isNotEmpty() }?.let { last ->
                        stringResource(R.string.workout_last_time, last.joinToString(", ") { Formatting.set(it.weightKg, it.reps, unit) })
                    }
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (card.suggestion?.increased == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onActions) { Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.action_more)) }
        }
        if (card.note.isNotBlank()) {
            Text(card.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable(onClick = onNote))
        }

        // Header row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderCell(stringResource(R.string.table_set), Modifier.width(34.dp))
            HeaderCell(stringResource(R.string.table_last), Modifier.weight(1.1f))
            HeaderCell(Formatting.symbol(unit), Modifier.weight(1f))
            HeaderCell(stringResource(R.string.table_reps), Modifier.weight(0.8f))
            Box(Modifier.width(40.dp))
        }

        val workingLast = card.lastTime
        var working = 0
        card.sets.forEachIndexed { setIndex, set ->
            val isWarmup = set.kind == SetKind.WARMUP
            if (!isWarmup) working++
            val previous = if (isWarmup) null else workingLast.getOrNull(working - 1)
            val label = when (set.kind) {
                SetKind.WARMUP -> stringResource(R.string.set_warmup_short)
                SetKind.DROP -> stringResource(R.string.set_drop_short)
                else -> "$working"
            }
            SetRow(
                label = label,
                warmup = isWarmup,
                previous = previous?.let { Formatting.set(it.weightKg, it.reps, unit) } ?: "–",
                weight = Formatting.weightNumber(set.weightKg, unit),
                reps = "${set.reps}",
                done = set.done,
                focusedField = focus?.takeIf { it.ref.exercise == index && it.ref.set == setIndex }?.field,
                onLabel = { onCycleKind(setIndex) },
                onWeight = { onCell(setIndex, Field.WEIGHT) },
                onReps = { onCell(setIndex, Field.REPS) },
                onToggle = { onToggle(setIndex) },
            )
            if (logRir && !isWarmup) {
                Text(
                    set.rir?.let { stringResource(R.string.rir_value, it) } ?: stringResource(R.string.rir_none),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (set.rir == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 42.dp).clickable { onRir(setIndex) }.padding(vertical = 2.dp),
                )
            }
        }
        TextButton(onClick = onAddSet) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_add_set))
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(
    label: String,
    warmup: Boolean,
    previous: String,
    weight: String,
    reps: String,
    done: Boolean,
    focusedField: Field?,
    onLabel: () -> Unit,
    onWeight: () -> Unit,
    onReps: () -> Unit,
    onToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            modifier = Modifier.width(34.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onLabel).padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (warmup) Knurl.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(previous, Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, maxLines = 1)
        ValueCell(weight, done, focusedField == Field.WEIGHT, Modifier.weight(1f), onWeight)
        ValueCell(reps, done, focusedField == Field.REPS, Modifier.weight(0.8f), onReps)
        val tickBg by animateColorAsState(if (done) Knurl.colors.success else MaterialTheme.colorScheme.surfaceContainerHigh, label = "tick")
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tickBg).clickable {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onToggle()
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = stringResource(if (done) R.string.set_undo else R.string.set_done),
                tint = if (done) Color.White else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ValueCell(text: String, done: Boolean, focused: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = when {
        focused -> MaterialTheme.colorScheme.surfaceContainerLowest
        done -> Knurl.colors.success.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1) }
}
