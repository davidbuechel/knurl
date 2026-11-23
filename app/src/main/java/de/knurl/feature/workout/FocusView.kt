package de.knurl.feature.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.knurl.R
import de.knurl.data.WeightUnit
import de.knurl.domain.SetRef
import de.knurl.domain.calc.Feel
import de.knurl.domain.model.SetKind
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.displayName
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.theme.Knurl

/** What the focus view does when tapped; the screen wires these to the ViewModel. */
class FocusActions(
    val changeWeight: (SetRef, Double) -> Unit,
    val changeReps: (SetRef, Int) -> Unit,
    val typeWeight: (SetRef) -> Unit,
    val typeReps: (SetRef) -> Unit,
    val done: (SetRef) -> Unit,
    val addRest: () -> Unit,
    val skipRest: () -> Unit,
    val feel: (Feel) -> Unit,
    val openExercise: (String) -> Unit,
    val showList: () -> Unit,
    val later: (Int) -> Unit,
    /** Puts a weight and reps into the set (from last time or the suggestion). */
    val apply: (SetRef, Double, Int) -> Unit,
    val finish: () -> Unit,
)

/**
 * The workout one set at a time, made for the gym: the exercise, the weight and reps in big type
 * with ± steps, and one large "Done". After a set the screen turns into the rest: a big countdown
 * and what comes next. The full set list is one tap away.
 */
@Composable
fun FocusView(s: ActiveWorkoutState, unit: WeightUnit, actions: FocusActions, modifier: Modifier = Modifier) {
    val resting = s.restLeftSec != null
    val mode = when {
        resting -> 1
        s.upNext != null -> 0
        else -> 2
    }
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
        label = "focus",
        modifier = modifier.fillMaxSize(),
    ) { current ->
        // The content sits at the top, the buttons at the bottom where the thumb is; on a small
        // screen the whole thing scrolls instead.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                when (current) {
                    0 -> s.upNext?.let { ref -> SetPanel(s, ref, unit, actions) }
                    1 -> RestPanel(s, unit, actions)
                    else -> AllDonePanel(actions)
                }
            }
        }
    }
}

@Composable
private fun ShowListButton(actions: FocusActions, modifier: Modifier = Modifier) {
    TextButton(onClick = actions.showList, modifier = modifier) {
        Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.focus_show_list), modifier = Modifier.padding(start = 6.dp))
    }
}

/** "80 kg × 8", or "12 reps" for bodyweight. */
@Composable
private fun setWithUnit(weightKg: Double, reps: Int, unit: WeightUnit): String =
    if (weightKg > 0) "${Formatting.weight(weightKg, unit)} × $reps" else "$reps ${stringResource(R.string.focus_reps_unit)}"

/** Where a set stands in its exercise: "Set 2 of 3", or "Warm-up". */
@Composable
private fun setLabel(card: ExerciseCard, set: Int): String {
    val entry = card.sets[set]
    if (entry.kind == SetKind.WARMUP) return stringResource(R.string.focus_warmup)
    val working = card.sets.count { it.kind != SetKind.WARMUP }
    val number = card.sets.take(set + 1).count { it.kind != SetKind.WARMUP }
    return stringResource(R.string.focus_set_of, number, working)
}

@Composable
private fun SetPanel(s: ActiveWorkoutState, ref: SetRef, unit: WeightUnit, actions: FocusActions) {
    val card = s.cards.getOrNull(ref.exercise) ?: return
    val set = card.sets.getOrNull(ref.set) ?: return
    val haptics = LocalHapticFeedback.current
    val bodyweight = set.weightKg == 0.0 && card.exercise?.equipment == de.knurl.domain.model.Equipment.BODYWEIGHT

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
    ExerciseHeader(card, setLabel(card, ref.set), actions)

    // Last time, or the step up the app suggests.
    val previous = card.lastTime.getOrNull(card.sets.take(ref.set + 1).count { it.kind != SetKind.WARMUP } - 1)
    // Tapping it puts those numbers in, when the set differs from them.
    val source: Pair<Double, Int>? = when {
        set.kind == SetKind.WARMUP -> null
        card.suggestion?.increased == true -> card.suggestion.weightKg to card.suggestion.reps
        previous != null -> previous.weightKg to previous.reps
        else -> null
    }
    val hint = when {
        set.kind == SetKind.WARMUP -> null
        card.suggestion?.increased == true -> stringResource(R.string.focus_suggest_up, setWithUnit(card.suggestion.weightKg, card.suggestion.reps, unit))
        previous != null -> stringResource(R.string.focus_last_time, setWithUnit(previous.weightKg, previous.reps, unit))
        set.weightKg == 0.0 && !bodyweight -> stringResource(R.string.focus_first_time)
        else -> null
    }
    if (hint != null) {
        val canApply = source != null && (source.first != set.weightKg || source.second != set.reps)
        Row(
            Modifier.clip(RoundedCornerShape(12.dp))
                .let { if (canApply) it.clickable { actions.apply(ref, source!!.first, source.second) } else it }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = if (card.suggestion?.increased == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (canApply) {
                Text(stringResource(R.string.focus_use), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!bodyweight) {
            BigValue(
                value = Formatting.weightNumber(set.weightKg, unit),
                unit = Formatting.symbol(unit),
                onMinus = { actions.changeWeight(ref, -card.weightStepKg) },
                onPlus = { actions.changeWeight(ref, card.weightStepKg) },
                onType = { actions.typeWeight(ref) },
                modifier = Modifier.weight(1f),
            )
        }
        BigValue(
            value = "${set.reps}",
            unit = stringResource(R.string.focus_reps_unit),
            onMinus = { actions.changeReps(ref, -1) },
            onPlus = { actions.changeReps(ref, 1) },
            onType = { actions.typeReps(ref) },
            modifier = Modifier.weight(1f),
        )
    }
    }

    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            actions.done(ref)
        },
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(28.dp))
        Text(stringResource(R.string.focus_done), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 10.dp))
    }

    SetDots(card, ref.set)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // Machine busy? Carry on with the next exercise and come back to this one. Only before the
        // exercise (and its superset) is started: the next set always follows the furthest one done.
        val block = de.knurl.domain.Supersets.blocks(s.cards.map { it.group }).firstOrNull { ref.exercise in it }
        val untouched = block != null && block.none { i -> s.cards[i].sets.any { it.done } }
        if (untouched && block.last < s.cards.lastIndex) {
            TextButton(onClick = { actions.later(ref.exercise) }) { Text(stringResource(R.string.focus_later)) }
        } else Spacer(Modifier.size(1.dp))
        ShowListButton(actions)
    }
    }
}

/** Photo, name and where we are; tapping opens the exercise. */
@Composable
private fun ExerciseHeader(card: ExerciseCard, label: String, actions: FocusActions) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { actions.openExercise(card.exerciseId) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        card.exercise?.let { ExerciseThumbnail(it, Modifier.size(64.dp)) }
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                card.exercise?.displayName() ?: card.exerciseId,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A number in big type with − and + below; tapping the number types it. */
@Composable
private fun BigValue(value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit, onType: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onType).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
            Text(unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
            }
            FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
            }
        }
    }
}

/** The sets of this exercise as dots: done, now, still to come. */
@Composable
private fun SetDots(card: ExerciseCard, now: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        card.sets.forEachIndexed { i, set ->
            val color = when {
                set.done -> Knurl.colors.success
                i == now -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                Modifier.padding(horizontal = 4.dp).size(width = if (i == now) 22.dp else 10.dp, height = 10.dp)
                    .clip(CircleShape).background(color),
            )
        }
    }
}

@Composable
private fun RestPanel(s: ActiveWorkoutState, unit: WeightUnit, actions: FocusActions) {
    val left = s.restLeftSec ?: 0
    val total = s.restTotalSec.coerceAtLeast(1)
    val fraction by animateFloatAsState((left.toFloat() / total).coerceIn(0f, 1f), tween(900), label = "rest")
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Text(stringResource(R.string.focus_rest).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(232.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(ring, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(Formatting.duration(left), style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = actions.addRest, modifier = Modifier.weight(1f).height(56.dp)) {
            Text(stringResource(R.string.rest_add_15), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = actions.skipRest, modifier = Modifier.weight(1f).height(56.dp)) {
            Text(stringResource(R.string.focus_skip_rest), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (s.askFeel) FeelQuestion(actions)
    }

    // What comes next, so the bar can be loaded during the rest.
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    s.upNext?.let { ref ->
        val card = s.cards.getOrNull(ref.exercise) ?: return@let
        val set = card.sets.getOrNull(ref.set) ?: return@let
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.focus_next).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                card.exercise?.let { ExerciseThumbnail(it, Modifier.size(48.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(card.exercise?.displayName() ?: card.exerciseId, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(setLabel(card, ref.set), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(setWithUnit(set.weightKg, set.reps, unit), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    ShowListButton(actions, Modifier.align(Alignment.CenterHorizontally))
    }
}

/** First time with an exercise: one tap tells the app whether the weight fits. */
@Composable
private fun FeelQuestion(actions: FocusActions) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.feel_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(stringResource(R.string.feel_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Feel.EASY to R.string.feel_easy, Feel.RIGHT to R.string.feel_right, Feel.HARD to R.string.feel_hard).forEach { (feel, label) ->
                Button(onClick = { actions.feel(feel) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(label), maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AllDonePanel(actions: FocusActions) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(Knurl.colors.success), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(56.dp))
        }
        Text(stringResource(R.string.focus_all_done), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.focus_all_done_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = actions.finish, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(24.dp)) {
            Text(stringResource(R.string.action_finish), style = MaterialTheme.typography.titleLarge)
        }
        ShowListButton(actions)
    }
}
