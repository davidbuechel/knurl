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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.core.notifications.Notifications
import de.knurl.domain.Supersets
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.feature.common.NoteDialog
import de.knurl.feature.common.Pill
import de.knurl.feature.common.appContainer
import de.knurl.feature.common.displayName
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.stepKg
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.brand.KnurlProgress
import de.knurl.ui.navigation.ExerciseRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.PickExerciseRoute
import de.knurl.ui.navigation.SummaryRoute
import de.knurl.ui.theme.Knurl

/** What the list shows, in order: the workout note, superset labels and exercises. */
private sealed interface Entry {
    data object Note : Entry
    data class SupersetLabel(val letter: Char, val firstExercise: Int) : Entry
    data class Exercise(val index: Int, val inSuperset: Boolean) : Entry
}

/** Which note is being written: the workout's (null) or an exercise's. */
private data class Noting(val exercise: Int?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(go: Navigator, savedState: SavedStateHandle, editWorkoutId: String? = null) {
    val vm = knurlViewModel(key = "workout-$editWorkoutId") { ActiveWorkoutViewModel(it.training, it.restTimer, editWorkoutId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current
    val editing = editWorkoutId != null
    val logRir by appContainer().preferences.logRir.collectAsStateWithLifecycle()
    if (!editing) {
        KeepScreenOn()
        AskForNotificationsOnce()
        RestOverFeedback(state?.restLeftSec, onDone = vm::restFinished)
    }
    // The library hands the picked exercise back here.
    LaunchedEffect(savedState) {
        savedState.getStateFlow<String?>(Navigator.RESULT_EXERCISE, null).collect { id ->
            if (id != null) {
                vm.addExercise(id)
                savedState[Navigator.RESULT_EXERCISE] = null
            }
        }
    }
    var leaving by remember { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<Int?>(null) }
    var noting by remember { mutableStateOf<Noting?>(null) }

    val s = state
    LaunchedEffect(s?.workout == null && s != null) { if (s != null && s.workout == null && !leaving) go.back() }

    // Number pad input: the first key replaces the value, further keys append.
    var buffer by remember(s?.focus) { mutableStateOf<String?>(null) }

    // List entries and the auto-scroll to the exercise with the focus.
    val entries = remember(s?.cards, s?.workout?.note) {
        buildList {
            if (!s?.workout?.note.isNullOrBlank()) add(Entry.Note)
            val cards = s?.cards.orEmpty()
            var letter = 'A'
            Supersets.blocks(cards.map { it.group }).forEach { block ->
                val superset = block.first != block.last
                if (superset) add(Entry.SupersetLabel(letter++, block.first))
                block.forEach { add(Entry.Exercise(it, superset)) }
            }
        }
    }
    val listState = rememberLazyListState()
    val focusedExercise = s?.focus?.ref?.exercise
    LaunchedEffect(focusedExercise) {
        val target = entries.indexOfFirst { it is Entry.Exercise && it.index == focusedExercise }
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Scaffold(
        topBar = {
            WorkoutHeader(
                title = s?.workout?.name.orEmpty(),
                subtitle = when {
                    s?.workout == null -> ""
                    editing -> Formatting.date(s.workout.startedAtMs)
                    else -> stringResource(R.string.workout_progress, Formatting.duration(s.elapsedSec), s.doneSets, s.totalSets)
                },
                progress = if (s == null || s.totalSets == 0) 0f else s.doneSets.toFloat() / s.totalSets,
                finishLabel = stringResource(if (editing) R.string.action_save else R.string.action_finish),
                onBack = go::back,
                onFinish = { confirmFinish = true },
                menuOpen = menuOpen,
                onMenu = { menuOpen = it },
                menu = {
                    DropdownMenuItem(text = { Text(stringResource(R.string.workout_note)) }, onClick = { menuOpen = false; noting = Noting(null) })
                    DropdownMenuItem(
                        text = { Text(stringResource(if (editing) R.string.action_discard_changes else R.string.action_discard_workout)) },
                        onClick = { menuOpen = false; confirmDiscard = true },
                    )
                },
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = s?.restLeftSec != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    RestBar(s?.restLeftSec ?: 0, s?.restTotalSec ?: 0, onAdd = { vm.addRest(15) }, onSkip = vm::skipRest)
                }
                val focus = s?.focus
                val card = focus?.let { s.cards.getOrNull(it.ref.exercise) }
                val set = card?.sets?.getOrNull(focus.ref.set)
                AnimatedVisibility(visible = set != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    if (focus != null && card != null && set != null) {
                        val weightField = focus.field == Field.WEIGHT
                        val stepKg = unit.stepKg(card.exercise?.equipment, card.weightStepKg)
                        val stepLabel = Formatting.kg(Formatting.toUnit(stepKg, unit).let { Math.round(it * 100) / 100.0 })
                        val setLabel = card.sets.take(focus.ref.set + 1).count { it.kind != de.knurl.domain.model.SetKind.WARMUP }
                        val name = card.exercise?.displayName() ?: card.exerciseId
                        NumberPad(
                            target = PadTarget(
                                title = stringResource(if (weightField) R.string.pad_weight else R.string.pad_reps, name, setLabel),
                                display = if (weightField) "${buffer ?: Formatting.weightNumber(set.weightKg, unit)} ${Formatting.symbol(unit)}" else buffer ?: "${set.reps}",
                                decimal = weightField,
                                minusLabel = if (weightField) "−$stepLabel" else "−1",
                                plusLabel = if (weightField) "+$stepLabel" else "+1",
                                nextLabel = stringResource(if (weightField) R.string.pad_next else R.string.pad_done_set),
                            ),
                            onDigit = { c ->
                                val next = (buffer ?: "") + c
                                if (weightField) {
                                    if (c == '.' && next.count { it == '.' } > 1) return@NumberPad
                                    if (next.length > 6) return@NumberPad
                                    buffer = next
                                    next.toDoubleOrNull()?.let { vm.setWeight(focus.ref.exercise, focus.ref.set, Formatting.fromUnit(it, unit)) }
                                } else {
                                    if (next.length > 3) return@NumberPad
                                    buffer = next
                                    next.toIntOrNull()?.let { vm.setReps(focus.ref.exercise, focus.ref.set, it) }
                                }
                            },
                            onBackspace = {
                                val base = buffer ?: if (weightField) Formatting.weightNumber(set.weightKg, unit).replace(',', '.') else "${set.reps}"
                                val next = base.dropLast(1)
                                buffer = next
                                if (weightField) vm.setWeight(focus.ref.exercise, focus.ref.set, Formatting.fromUnit(next.toDoubleOrNull() ?: 0.0, unit))
                                else vm.setReps(focus.ref.exercise, focus.ref.set, next.toIntOrNull() ?: 0)
                            },
                            onMinus = {
                                buffer = null
                                if (weightField) vm.changeWeight(focus.ref.exercise, focus.ref.set, -stepKg) else vm.changeReps(focus.ref.exercise, focus.ref.set, -1)
                            },
                            onPlus = {
                                buffer = null
                                if (weightField) vm.changeWeight(focus.ref.exercise, focus.ref.set, stepKg) else vm.changeReps(focus.ref.exercise, focus.ref.set, 1)
                            },
                            onNext = vm::next,
                            onClose = vm::clearFocus,
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (s?.workout == null) return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { e ->
                when (e) {
                    Entry.Note -> "note"
                    is Entry.SupersetLabel -> "sup-${e.firstExercise}"
                    is Entry.Exercise -> "ex-${e.index}-${s.cards.getOrNull(e.index)?.exerciseId}"
                }
            }) { entry ->
                when (entry) {
                    Entry.Note -> Text(
                        s.workout.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { noting = Noting(null) },
                    )
                    is Entry.SupersetLabel -> Pill(stringResource(R.string.superset_label, entry.letter.toString()), color = Knurl.colors.superset)
                    is Entry.Exercise -> {
                        val card = s.cards[entry.index]
                        val block: @Composable (Modifier) -> Unit = { modifier ->
                            Box(modifier) {
                                ExerciseBlock(
                                    card = card,
                                    index = entry.index,
                                    focus = s.focus,
                                    logRir = logRir,
                                    onOpenExercise = { go.to(ExerciseRoute(card.exerciseId)) },
                                    onActions = { actionsFor = entry.index },
                                    onCell = { set, field -> vm.focus(entry.index, set, field) },
                                    onToggle = { set -> vm.toggleDone(entry.index, set) },
                                    onCycleKind = { set -> vm.cycleKind(entry.index, set) },
                                    onRir = { set -> vm.cycleRir(entry.index, set) },
                                    onAddSet = { vm.addSet(entry.index) },
                                    onNote = { noting = Noting(entry.index) },
                                )
                            }
                        }
                        if (entry.inSuperset) {
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(4.dp).fillMaxHeight().background(Knurl.colors.superset, RoundedCornerShape(2.dp)))
                                block(Modifier.weight(1f))
                            }
                        } else block(Modifier)
                    }
                }
            }
            if (s.cards.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_workout_title), stringResource(R.string.empty_workout_body)) }
            }
            item {
                OutlinedButton(onClick = { go.to(PickExerciseRoute) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(stringResource(R.string.action_add_exercise))
                }
            }
            if (!editing && s.totalSets > 0 && s.doneSets == s.totalSets) {
                item {
                    Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(stringResource(R.string.workout_all_done))
                    }
                }
            }
        }
    }

    actionsFor?.let { index ->
        val card = s?.cards?.getOrNull(index)
        if (card == null) {
            actionsFor = null
        } else {
            val inSuperset = card.group != null
            ModalBottomSheet(onDismissRequest = { actionsFor = null }) {
                Text(
                    card.exercise?.displayName() ?: card.exerciseId,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Column(Modifier.padding(bottom = 24.dp)) {
                    ListRow(stringResource(R.string.action_add_warmups), leading = { IconTile(Icons.Rounded.Whatshot, Knurl.colors.warning) }) {
                        vm.addWarmups(index); actionsFor = null
                    }
                    ListRow(stringResource(R.string.exercise_note), leading = { IconTile(Icons.AutoMirrored.Rounded.Notes) }) {
                        actionsFor = null; noting = Noting(index)
                    }
                    if (inSuperset) {
                        ListRow(stringResource(R.string.superset_leave), leading = { IconTile(Icons.Rounded.LinkOff, Knurl.colors.superset) }) {
                            vm.unlink(index); actionsFor = null
                        }
                    }
                    if (index < (s.cards.lastIndex)) {
                        ListRow(stringResource(R.string.superset_link_next), leading = { IconTile(Icons.Rounded.Link, Knurl.colors.superset) }) {
                            vm.linkWithNext(index); actionsFor = null
                        }
                    }
                    ListRow(stringResource(R.string.action_remove_last_set), leading = { IconTile(Icons.Rounded.RemoveCircleOutline, MaterialTheme.colorScheme.outline) }) {
                        vm.removeSet(index, card.sets.lastIndex); actionsFor = null
                    }
                    ListRow(stringResource(R.string.action_remove_exercise), leading = { IconTile(Icons.Rounded.DeleteOutline, MaterialTheme.colorScheme.error) }, divider = false) {
                        vm.removeExercise(index); actionsFor = null
                    }
                }
            }
        }
    }

    noting?.let { n ->
        val current = if (n.exercise == null) s?.workout?.note.orEmpty() else s?.cards?.getOrNull(n.exercise)?.note.orEmpty()
        NoteDialog(
            title = stringResource(if (n.exercise == null) R.string.workout_note else R.string.exercise_note),
            initial = current,
            onDismiss = { noting = null },
            onConfirm = { text ->
                if (n.exercise == null) vm.setWorkoutNote(text) else vm.setExerciseNote(n.exercise, text)
                noting = null
            },
        )
    }
    if (confirmFinish) {
        val undone = s?.let { it.totalSets - it.doneSets } ?: 0
        ConfirmDialog(
            title = stringResource(if (editing) R.string.edit_save_title else R.string.finish_title),
            body = if (undone > 0) stringResource(R.string.finish_body_undone, undone) else stringResource(R.string.finish_body),
            confirm = stringResource(if (editing) R.string.action_save else R.string.action_finish),
            onDismiss = { confirmFinish = false },
            onConfirm = {
                confirmFinish = false
                leaving = true
                val savedId = vm.finish()
                if (!editing && savedId != null) go.replace(SummaryRoute(savedId)) else go.back()
            },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(if (editing) R.string.edit_discard_title else R.string.discard_title),
            body = stringResource(if (editing) R.string.edit_discard_body else R.string.discard_body),
            confirm = stringResource(R.string.action_discard),
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; leaving = true; vm.discard(); go.back() },
        )
    }
}

/** Top of the workout: back, name, time and sets, the plates filling up with the workout, finish. */
@Composable
private fun WorkoutHeader(
    title: String,
    subtitle: String,
    progress: Float,
    finishLabel: String,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    menu: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            KnurlProgress(progress, Modifier.size(34.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Button(onClick = onFinish) { Text(finishLabel) }
            Box {
                IconButton(onClick = { onMenu(true) }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenu(false) }) { menu() }
            }
        }
    }
}

/** Rest between sets: the plates load up as the rest runs out, so "full" means "go". */
@Composable
private fun RestBar(leftSec: Long, totalSec: Long, onAdd: () -> Unit, onSkip: () -> Unit) {
    val progress = if (totalSec <= 0) 0f else 1f - leftSec.toFloat() / totalSec
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shadowElevation = 8.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KnurlProgress(progress, Modifier.size(40.dp), color = MaterialTheme.colorScheme.inversePrimary)
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
