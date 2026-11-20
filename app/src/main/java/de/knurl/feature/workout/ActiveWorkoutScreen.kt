package de.knurl.feature.workout

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.core.notifications.Notifications
import de.knurl.feature.common.stepKg
import de.knurl.feature.common.weightText
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.domain.model.SetKind
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.appContainer
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
    val vm = knurlViewModel { ActiveWorkoutViewModel(it.training, it.restTimer) }
    val state by vm.state.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current
    KeepScreenOn()
    AskForNotificationsOnce()
    RestOverFeedback(state?.restLeftSec, onDone = vm::restFinished)
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
            NumberInputDialog(stringResource(R.string.workout_weight, Formatting.symbol(unit)), Formatting.weightNumber(set.weightKg, unit), decimal = true,
                onDismiss = { typing = null }, onConfirm = { vm.setWeight(t.exercise, t.set, Formatting.fromUnit(it, unit)); typing = null })
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
    val unit = LocalWeightUnit.current
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
                stringResource(R.string.workout_last_time, card.lastTime.joinToString(", ") { Formatting.set(it.weightKg, it.reps, unit) }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        card.suggestion?.takeIf { it.increased }?.let {
            Text(
                stringResource(R.string.workout_suggest_increase, weightText(it.weightKg)),
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
                weight = weightText(set.weightKg),
                reps = "${set.reps}",
                done = set.done,
                onLabel = { onCycleKind(index) },
                onWeightMinus = { onWeight(index, -unit.stepKg(card.exercise?.equipment, card.weightStepKg)) },
                onWeightPlus = { onWeight(index, unit.stepKg(card.exercise?.equipment, card.weightStepKg)) },
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

/** Keeps the display on while this screen is shown, if the person wants that (Settings). */
@Composable
private fun KeepScreenOn() {
    val wanted by appContainer().preferences.keepScreenOn.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(wanted) {
        view.keepScreenOn = wanted
        onDispose { view.keepScreenOn = false }
    }
}

/** In the app, the end of rest is a vibration and/or a short tone (Settings); the alarm covers the background. */
@Composable
private fun RestOverFeedback(restLeftSec: Long?, onDone: () -> Unit) {
    val prefs = appContainer().preferences
    val context = LocalContext.current
    LaunchedEffect(restLeftSec == 0L) {
        if (restLeftSec != 0L) return@LaunchedEffect
        if (prefs.restVibrate.value) {
            context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        }
        if (prefs.restSound.value) {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        }
        onDone()
    }
}

/** The rest timer and the workout notification need the permission: ask once, the first time a workout runs. */
@Composable
private fun AskForNotificationsOnce() {
    val prefs = appContainer().preferences
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !prefs.notificationsAsked && !Notifications.permissionGranted(context)) {
            prefs.notificationsAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
