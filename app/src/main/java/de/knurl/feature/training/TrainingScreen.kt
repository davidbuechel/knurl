package de.knurl.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.NameDialog
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.tonnageText
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.ActiveWorkoutRoute
import de.knurl.ui.navigation.EditWorkoutRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.RoutineEditorRoute
import kotlinx.coroutines.launch

/** Something waiting for a name: a duplicate of a routine or a workout to keep as routine. */
private sealed interface Naming {
    data class Duplicate(val routine: Routine) : Naming
    data class FromWorkout(val workout: Workout) : Naming
}

@Composable
fun TrainingScreen(go: Navigator) {
    val vm = knurlViewModel { TrainingViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val german = isGerman()
    val freeName = stringResource(R.string.workout_free_name)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf<Naming?>(null) }
    val deletedText = stringResource(R.string.deleted)
    val undoText = stringResource(R.string.action_undo)

    /** Deletes at once and offers to undo, instead of asking first. */
    fun deleteWithUndo(delete: () -> Unit, restore: () -> Unit) {
        delete()
        scope.launch {
            if (snackbar.showSnackbar(deletedText, undoText, withDismissAction = true) == SnackbarResult.ActionPerformed) restore()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenTitle(stringResource(R.string.tab_training)) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { vm.start(null, freeName); go.to(ActiveWorkoutRoute) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(stringResource(if (s.hasActive) R.string.action_resume else R.string.action_start_free))
                    }
                    OutlinedButton(onClick = { go.to(RoutineEditorRoute()) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(R.string.routine_new))
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.training_routines), Modifier.padding(top = 8.dp)) }
            if (s.routines.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.routines_empty_title), stringResource(R.string.routines_empty_body)) }
            }
            items(s.routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    summary = routine.items.joinToString(" · ") { vm.exerciseName(it.exerciseId, german) },
                    first = routine == s.routines.first(),
                    last = routine == s.routines.last(),
                    onStart = { vm.start(routine, freeName); go.to(ActiveWorkoutRoute) },
                    onEdit = { go.to(RoutineEditorRoute(routine.id)) },
                    onMove = { by -> vm.moveRoutine(routine, by) },
                    onDuplicate = { naming = Naming.Duplicate(routine) },
                    onDelete = { deleteWithUndo({ vm.deleteRoutine(routine) }, { vm.restoreRoutine(routine) }) },
                )
            }

            item { SectionLabel(stringResource(R.string.training_history), Modifier.padding(top = 8.dp)) }
            if (s.history.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_history_title), stringResource(R.string.empty_history_body)) }
            }
            items(s.history, key = { it.workout.id }) { row ->
                HistoryCard(
                    row = row,
                    onOpen = { go.to(EditWorkoutRoute(row.workout.id)) },
                    onSaveAsRoutine = { naming = Naming.FromWorkout(row.workout) },
                    onDelete = { deleteWithUndo({ vm.deleteWorkout(row.workout) }, { vm.restoreWorkout(row.workout) }) },
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    when (val n = naming) {
        is Naming.Duplicate -> NameDialog(
            stringResource(R.string.routine_duplicate), stringResource(R.string.routine_copy_name, n.routine.name),
            onDismiss = { naming = null }, onConfirm = { vm.duplicateRoutine(n.routine, it); naming = null },
        )
        is Naming.FromWorkout -> NameDialog(
            stringResource(R.string.summary_save_routine), n.workout.name,
            onDismiss = { naming = null }, onConfirm = { vm.saveAsRoutine(n.workout, it); naming = null },
        )
        null -> Unit
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    summary: String,
    first: Boolean,
    last: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    KnurlCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleLarge)
                Text(summary.ifEmpty { stringResource(R.string.routine_empty_title) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.routine_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.routine_move_up)) }, enabled = !first, onClick = { menu = false; onMove(-1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.routine_move_down)) }, enabled = !last, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.routine_duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
            FilledTonalIconButton(onClick = onStart, enabled = routine.items.isNotEmpty()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start_workout))
            }
        }
    }
}

@Composable
private fun HistoryCard(row: HistoryRow, onOpen: () -> Unit, onSaveAsRoutine: () -> Unit, onDelete: () -> Unit) {
    var menu by rememberSaveable { mutableStateOf(false) }
    KnurlCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.history_line, Formatting.date(row.workout.startedAtMs), Formatting.duration(row.durationSec), row.sets, tonnageText(row.volumeKg)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.history_edit)) }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.summary_save_routine)) }, onClick = { menu = false; onSaveAsRoutine() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
