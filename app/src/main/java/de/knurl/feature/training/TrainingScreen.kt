package de.knurl.feature.training

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.ui.navigation.CustomExerciseRoute
import de.knurl.feature.exercises.ExercisesScreen
import de.knurl.ui.theme.Knurl
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import de.knurl.domain.calc.Estimates
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.NameDialog
import de.knurl.feature.common.Pill
import de.knurl.feature.common.SegmentedTabs
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.tonnageText
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.ActiveWorkoutRoute
import de.knurl.ui.navigation.EditWorkoutRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.RoutineEditorRoute
import kotlinx.coroutines.launch

private enum class Tab { ROUTINES, HISTORY, EXERCISES }

private sealed interface Naming {
    data class Duplicate(val routine: Routine) : Naming
    data class FromWorkout(val workout: Workout) : Naming
}

private sealed interface Sheet {
    data class ForRoutine(val routine: Routine) : Sheet
    data class ForWorkout(val workout: Workout) : Sheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(go: Navigator) {
    val vm = knurlViewModel { TrainingViewModel(it.training, it.restTimer) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val freeName = stringResource(R.string.workout_free_name)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.ROUTINES) }
    var naming by remember { mutableStateOf<Naming?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    // Starting a routine while another workout runs asks first instead of quietly resuming that one.
    var replacing by remember { mutableStateOf<Routine?>(null) }
    fun startRoutine(routine: Routine) {
        if (s.hasActive) replacing = routine else { vm.start(routine, freeName); go.to(ActiveWorkoutRoute) }
    }
    val deletedText = stringResource(R.string.deleted)
    val undoText = stringResource(R.string.action_undo)

    fun deleteWithUndo(delete: () -> Unit, restore: () -> Unit) {
        delete()
        scope.launch { if (snackbar.showSnackbar(deletedText, undoText, withDismissAction = true) == SnackbarResult.ActionPerformed) restore() }
    }

    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        // Title and tabs stay put; the button adds what the tab is about.
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_training), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                if (tab != Tab.HISTORY) {
                    FilledTonalButton(onClick = { if (tab == Tab.EXERCISES) go.to(CustomExerciseRoute()) else go.to(RoutineEditorRoute()) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(if (tab == Tab.EXERCISES) R.string.custom_add else R.string.routine_short))
                    }
                }
            }
            SegmentedTabs(Tab.entries, tab, {
                stringResource(when (it) { Tab.ROUTINES -> R.string.training_routines; Tab.HISTORY -> R.string.training_history; Tab.EXERCISES -> R.string.tab_exercises })
            }, { tab = it })
        }
        if (tab == Tab.EXERCISES) {
            ExercisesScreen(go, pickForWorkout = false, embedded = true)
        } else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (tab == Tab.ROUTINES) {
                // A running workout comes first, so it is never mistaken for one still to start.
                s.activeName?.let { name ->
                    item(key = "active") {
                        ListGroup {
                            ListRow(
                                name,
                                subtitle = stringResource(R.string.today_running),
                                leading = { IconTile(Icons.Rounded.PlayArrow, Knurl.colors.success) },
                                trailing = { Text(stringResource(R.string.action_resume_short), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                                divider = false,
                                onClick = { go.to(ActiveWorkoutRoute) },
                            )
                        }
                    }
                }
                if (s.routines.isEmpty()) {
                    item { BrandEmptyState(stringResource(R.string.routines_empty_title), stringResource(R.string.routines_empty_body)) }
                }
                items(s.routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        next = routine.id == s.nextRoutineId,
                        exercises = routine.items.mapNotNull { vm.exercise(it.exerciseId) },
                        onOpen = { go.to(RoutineEditorRoute(routine.id)) },
                        onLongPress = { sheet = Sheet.ForRoutine(routine) },
                        onStart = { startRoutine(routine) },
                    )
                }
                // Same as on Today: a quiet way to start without a routine, only while nothing runs.
                if (!s.hasActive) item {
                    TextButton(onClick = { vm.start(null, freeName); go.to(ActiveWorkoutRoute) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.action_start_empty))
                    }
                }
            } else {
                if (s.history.isEmpty()) {
                    item { BrandEmptyState(stringResource(R.string.empty_history_title), stringResource(R.string.empty_history_body)) }
                } else {
                    item {
                        ListGroup {
                            s.history.forEachIndexed { i, row ->
                                ListRow(
                                    title = row.workout.name,
                                    subtitle = listOfNotNull(Formatting.date(row.workout.startedAtMs), Formatting.length(row.durationSec), tonnageText(row.volumeKg), row.kcal?.let { stringResource(R.string.kcal_value, Formatting.kg(it.toDouble())) }).joinToString(" · "),
                                    trailing = if (row.records > 0) ({ Pill(stringResource(R.string.record_count, row.records), color = MaterialTheme.colorScheme.tertiary) }) else null,
                                    divider = i < s.history.lastIndex,
                                    onClick = { sheet = Sheet.ForWorkout(row.workout) },
                                )
                            }
                        }
                    }
                }
            }
        }
      }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    when (val current = sheet) {
        is Sheet.ForRoutine -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            val r = current.routine
            val index = s.routines.indexOfFirst { it.id == r.id }
            SheetTitle(r.name)
            Column(Modifier.padding(bottom = 24.dp)) {
                ListRow(stringResource(R.string.action_start_workout), leading = { IconTile(Icons.Rounded.PlayArrow) }) { sheet = null; startRoutine(r) }
                ListRow(stringResource(R.string.routine_edit), leading = { IconTile(Icons.Rounded.Edit) }) { sheet = null; go.to(RoutineEditorRoute(r.id)) }
                if (index > 0) ListRow(stringResource(R.string.routine_move_up), leading = { IconTile(Icons.Rounded.KeyboardArrowUp, MaterialTheme.colorScheme.outline) }) { sheet = null; vm.moveRoutine(r, -1) }
                if (index < s.routines.lastIndex) ListRow(stringResource(R.string.routine_move_down), leading = { IconTile(Icons.Rounded.KeyboardArrowDown, MaterialTheme.colorScheme.outline) }) { sheet = null; vm.moveRoutine(r, 1) }
                ListRow(stringResource(R.string.routine_duplicate), leading = { IconTile(Icons.Rounded.ContentCopy, MaterialTheme.colorScheme.outline) }) { sheet = null; naming = Naming.Duplicate(r) }
                ListRow(stringResource(R.string.action_delete), leading = { IconTile(Icons.Rounded.DeleteOutline, MaterialTheme.colorScheme.error) }, divider = false) {
                    sheet = null; deleteWithUndo({ vm.deleteRoutine(r) }, { vm.restoreRoutine(r) })
                }
            }
        }
        is Sheet.ForWorkout -> ModalBottomSheet(onDismissRequest = { sheet = null }) {
            val w = current.workout
            SheetTitle(w.name, Formatting.date(w.startedAtMs))
            Column(Modifier.padding(bottom = 24.dp)) {
                ListRow(stringResource(R.string.history_edit), leading = { IconTile(Icons.Rounded.Edit) }) { sheet = null; go.to(EditWorkoutRoute(w.id)) }
                ListRow(stringResource(R.string.summary_save_routine), leading = { IconTile(Icons.AutoMirrored.Rounded.PlaylistAdd) }) { sheet = null; naming = Naming.FromWorkout(w) }
                ListRow(stringResource(R.string.action_delete), leading = { IconTile(Icons.Rounded.DeleteOutline, MaterialTheme.colorScheme.error) }, divider = false) {
                    sheet = null; deleteWithUndo({ vm.deleteWorkout(w) }, { vm.restoreWorkout(w) })
                }
            }
        }
        null -> Unit
    }

    replacing?.let { routine ->
        AlertDialog(
            onDismissRequest = { replacing = null },
            title = { Text(stringResource(R.string.active_running_title)) },
            text = { Text(stringResource(R.string.active_running_body, s.activeName.orEmpty(), routine.name)) },
            confirmButton = { TextButton(onClick = { replacing = null; go.to(ActiveWorkoutRoute) }) { Text(stringResource(R.string.action_resume_short)) } },
            dismissButton = {
                TextButton(onClick = { replacing = null; vm.replaceActive(routine, freeName); go.to(ActiveWorkoutRoute) }) {
                    Text(stringResource(R.string.active_replace), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    when (val n = naming) {
        is Naming.Duplicate -> NameDialog(stringResource(R.string.routine_duplicate), stringResource(R.string.routine_copy_name, n.routine.name),
            onDismiss = { naming = null }, onConfirm = { vm.duplicateRoutine(n.routine, it); naming = null })
        is Naming.FromWorkout -> NameDialog(stringResource(R.string.summary_save_routine), n.workout.name,
            onDismiss = { naming = null }, onConfirm = { vm.saveAsRoutine(n.workout, it); naming = null })
        null -> Unit
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineCard(
    routine: Routine,
    next: Boolean,
    exercises: List<de.knurl.domain.model.Exercise>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(2.dp, if (next) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.large)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (next) Pill(stringResource(R.string.routine_next))
                Text(routine.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(
                        pluralStringResource(R.plurals.exercises_count, routine.items.size, routine.items.size),
                        pluralStringResource(R.plurals.sets_count, Estimates.sets(routine), Estimates.sets(routine)),
                        stringResource(R.string.minutes_about, Estimates.minutes(routine)),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(onClick = onStart, enabled = routine.items.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start_workout))
            }
        }
        if (exercises.isNotEmpty()) {
            Row {
                exercises.take(6).forEachIndexed { i, e ->
                    Box(Modifier.offset(x = (-8 * i).dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(2.dp)) {
                        ExerciseThumbnail(e, Modifier.size(36.dp).clip(CircleShape))
                    }
                }
                if (exercises.size > 6) {
                    Text("+${exercises.size - 6}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(x = (-8 * 6 + 6).dp).align(Alignment.CenterVertically))
                }
            }
        }
    }
}
