package de.knurl.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.domain.model.SetKind
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.Stepper
import de.knurl.feature.common.displayName
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.PickExerciseRoute
import de.knurl.ui.theme.Knurl

/** Which number the type-in dialog is editing. */
private data class Typing(val exercise: Int, val set: Int, val weight: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(go: Navigator) {
    val vm = knurlViewModel { ActiveWorkoutViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var typing by remember { mutableStateOf<Typing?>(null) }

    val s = state
    // Finished or discarded (here or elsewhere): there is nothing left to show.
    LaunchedEffect(s?.workout == null && s != null) { if (s != null && s.workout == null) go.back() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s?.workout?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            Formatting.duration(s?.elapsedSec ?: 0),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    Button(onClick = { confirmFinish = true }) { Text(stringResource(R.string.action_finish)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_discard_workout)) },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { go.to(PickExerciseRoute) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_exercise)) },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = s?.restLeftSec != null) {
                RestBar(s?.restLeftSec ?: 0, onAdd = { vm.addRest(15) }, onSkip = vm::skipRest)
            }
        },
    ) { padding ->
        if (s?.workout == null) return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s.cards.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_workout_title), stringResource(R.string.empty_workout_body)) }
            }
            itemsIndexed(s.cards) { exerciseIndex, card ->
                ExerciseCardView(
                    card = card,
                    onWeight = { set, delta -> vm.changeWeight(exerciseIndex, set, delta) },
                    onReps = { set, delta -> vm.changeReps(exerciseIndex, set, delta) },
                    onType = { set, weight -> typing = Typing(exerciseIndex, set, weight) },
                    onToggle = { set -> vm.toggleDone(exerciseIndex, set) },
                    onCycleKind = { set -> vm.cycleKind(exerciseIndex, set) },
                    onAddSet = { vm.addSet(exerciseIndex) },
                    onRemoveSet = { set -> vm.removeSet(exerciseIndex, set) },
                    onRemoveExercise = { vm.removeExercise(exerciseIndex) },
                )
            }
        }
    }

    typing?.let { t ->
        val set = s?.cards?.getOrNull(t.exercise)?.sets?.getOrNull(t.set)
        if (set != null && t.weight) {
            NumberInputDialog(stringResource(R.string.workout_weight_kg), Formatting.kg(set.weightKg), decimal = true,
                onDismiss = { typing = null }, onConfirm = { vm.setWeight(t.exercise, t.set, it); typing = null })
        } else if (set != null) {
            NumberInputDialog(stringResource(R.string.workout_reps), "${set.reps}", decimal = false,
                onDismiss = { typing = null }, onConfirm = { vm.setReps(t.exercise, t.set, it.toInt()); typing = null })
        }
    }
    if (confirmFinish) {
        val undone = s?.cards?.sumOf { c -> c.sets.count { !it.done } } ?: 0
        ConfirmDialog(
            title = stringResource(R.string.finish_title),
            body = if (undone > 0) stringResource(R.string.finish_body_undone, undone) else stringResource(R.string.finish_body),
            confirm = stringResource(R.string.action_finish),
            onDismiss = { confirmFinish = false },
            onConfirm = { confirmFinish = false; vm.finish() },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            body = stringResource(R.string.discard_body),
            confirm = stringResource(R.string.action_discard),
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; vm.discard() },
        )
    }
}

@Composable
private fun ExerciseCardView(
    card: ExerciseCard,
    onWeight: (Int, Double) -> Unit,
    onReps: (Int, Int) -> Unit,
    onType: (Int, Boolean) -> Unit,
    onToggle: (Int) -> Unit,
    onCycleKind: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    KnurlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.exercise?.displayName() ?: card.exerciseId, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove_last_set)) },
                        enabled = card.sets.isNotEmpty(),
                        onClick = { menuOpen = false; onRemoveSet(card.sets.lastIndex) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove_exercise)) },
                        onClick = { menuOpen = false; onRemoveExercise() },
                    )
                }
            }
        }

        if (card.lastTime.isNotEmpty()) {
            Text(
                stringResource(R.string.workout_last_time, card.lastTime.joinToString(", ") { Formatting.set(it.weightKg, it.reps) }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        card.suggestion?.takeIf { it.increased }?.let {
            Text(
                stringResource(R.string.workout_suggest_increase, Formatting.kg(it.weightKg)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        card.sets.forEachIndexed { index, set ->
            val workingNumber = card.sets.take(index + 1).count { it.kind != SetKind.WARMUP }
            SetRow(
                label = when (set.kind) {
                    SetKind.WARMUP -> stringResource(R.string.set_warmup_short)
                    SetKind.DROP -> stringResource(R.string.set_drop_short)
                    else -> "$workingNumber"
                },
                warmup = set.kind == SetKind.WARMUP,
                weight = stringResource(R.string.unit_kg_value, Formatting.kg(set.weightKg)),
                reps = "${set.reps}",
                done = set.done,
                onLabel = { onCycleKind(index) },
                onWeightMinus = { onWeight(index, -card.weightStepKg) },
                onWeightPlus = { onWeight(index, card.weightStepKg) },
                onWeightType = { onType(index, true) },
                onRepsMinus = { onReps(index, -1) },
                onRepsPlus = { onReps(index, 1) },
                onRepsType = { onType(index, false) },
                onToggle = { onToggle(index) },
            )
        }

        TextButton(onClick = onAddSet) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(R.string.action_add_set))
        }
    }
}

@Composable
private fun SetRow(
    label: String,
    warmup: Boolean,
    weight: String,
    reps: String,
    done: Boolean,
    onLabel: () -> Unit,
    onWeightMinus: () -> Unit,
    onWeightPlus: () -> Unit,
    onWeightType: () -> Unit,
    onRepsMinus: () -> Unit,
    onRepsPlus: () -> Unit,
    onRepsType: () -> Unit,
    onToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            modifier = Modifier.width(24.dp).clickable(onClick = onLabel),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (warmup) Knurl.colors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Stepper(weight, onWeightMinus, onWeightPlus, onWeightType, Modifier.weight(1.25f), highlighted = done)
        Stepper(reps, onRepsMinus, onRepsPlus, onRepsType, Modifier.weight(1f), highlighted = done)
        val tick = if (done) Knurl.colors.success else MaterialTheme.colorScheme.surfaceContainer
        Box(
            Modifier
                .size(40.dp)
                .background(tick, RoundedCornerShape(10.dp))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = stringResource(if (done) R.string.set_undo else R.string.set_done),
                tint = if (done) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun RestBar(leftSec: Long, onAdd: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rest_label), style = MaterialTheme.typography.labelMedium)
                Text(Formatting.duration(leftSec), style = MaterialTheme.typography.headlineMedium)
            }
            FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.rest_add_15)) }
            IconButton(onClick = onSkip) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.rest_skip)) }
        }
    }
}
