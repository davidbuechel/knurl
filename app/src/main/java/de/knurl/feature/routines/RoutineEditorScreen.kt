package de.knurl.feature.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.feature.common.Pill
import de.knurl.ui.theme.Knurl
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.RoutineEditing
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.Stepper
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.subtitle
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.PickExerciseRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Edits a copy of the routine; nothing is saved until "Save". */
class RoutineEditorViewModel(private val training: TrainingRepository, routineId: String?, defaultName: String) : ViewModel() {

    private val defaultRest = training.data.value?.settings?.defaultRestSec ?: 120

    private val original = routineId?.let(training::routine) ?: training.newRoutine(defaultName)
    private val _routine = MutableStateFlow(original)
    val routine: StateFlow<Routine> = _routine.asStateFlow()

    /** True once something differs from what is saved (a new routine counts once it has exercises). */
    fun hasChanges(): Boolean = _routine.value != original && (_routine.value.items.isNotEmpty() || _routine.value.name != original.name)

    fun exercise(id: String) = training.exercise(id)

    fun rename(name: String) = _routine.update { it.copy(name = name.take(80)) }
    fun add(exerciseId: String) = _routine.update { RoutineEditing.add(it, exerciseId, defaultRest) }
    fun remove(index: Int) = _routine.update { RoutineEditing.remove(it, index) }
    fun move(index: Int, by: Int) = _routine.update { RoutineEditing.move(it, index, by) }
    fun update(index: Int, transform: (RoutineItem) -> RoutineItem) = _routine.update { RoutineEditing.update(it, index, transform) }
    fun linkWithNext(index: Int) = _routine.update { RoutineEditing.linkWithNext(it, index) }
    fun unlink(index: Int) = _routine.update { RoutineEditing.unlink(it, index) }

    fun save() = training.saveRoutine(_routine.value.copy(name = _routine.value.name.trim()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(go: Navigator, routineId: String?, savedState: SavedStateHandle) {
    val defaultName = stringResource(R.string.routine_new_name)
    val vm = knurlViewModel(key = "routine-$routineId") { RoutineEditorViewModel(it.training, routineId, defaultName) }
    val routine by vm.routine.collectAsStateWithLifecycle()
    val german = isGerman()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    // The field owns its text (a flow-bound field can lose letters when typing fast).
    var name by rememberSaveable { mutableStateOf(routine.name) }
    val leave = { if (vm.hasChanges()) confirmDiscard = true else go.back() }
    // Only with unsaved changes: otherwise the back gesture plays its normal animation.
    BackHandler(enabled = vm.hasChanges()) { confirmDiscard = true }

    LaunchedEffect(savedState) {
        savedState.getStateFlow<String?>(Navigator.RESULT_EXERCISE, null).collect { id ->
            if (id != null) {
                vm.add(id)
                savedState[Navigator.RESULT_EXERCISE] = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (routineId == null) R.string.routine_new else R.string.routine_edit)) },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    Button(
                        onClick = { vm.save(); go.back() },
                        enabled = routine.name.isNotBlank() && routine.items.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.action_save)) }
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
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80); vm.rename(it) },
                    label = { Text(stringResource(R.string.routine_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
            if (routine.items.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.routine_empty_title), stringResource(R.string.routine_empty_body)) }
            }
            itemsIndexed(routine.items) { index, item ->
                val exercise = vm.exercise(item.exerciseId)
                val startsSuperset = item.group != null && routine.items.getOrNull(index - 1)?.group != item.group
                if (startsSuperset) {
                    val letter = 'A' + (routine.items.take(index + 1).mapNotNull { it.group }.distinct().size - 1)
                    Pill(stringResource(R.string.superset_label, letter.toString()), color = Knurl.colors.superset, modifier = Modifier.padding(bottom = 6.dp))
                }
                val card: @Composable (Modifier) -> Unit = { cardModifier -> KnurlCard(cardModifier) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        exercise?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(exercise?.name(german) ?: item.exerciseId, style = MaterialTheme.typography.titleMedium)
                            exercise?.let { Text(it.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        IconButton(onClick = { vm.move(index, -1) }, enabled = index > 0) {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.routine_move_up))
                        }
                        IconButton(onClick = { vm.move(index, 1) }, enabled = index < routine.items.lastIndex) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.routine_move_down))
                        }
                        IconButton(onClick = { vm.remove(index) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove_exercise))
                        }
                    }
                    EditorRow(stringResource(R.string.routine_sets)) {
                        Stepper("${item.sets}", { vm.update(index) { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } }, { vm.update(index) { it.copy(sets = (it.sets + 1).coerceAtMost(10)) } })
                    }
                    EditorRow(stringResource(R.string.routine_reps)) {
                        Stepper(
                            "${item.repRange.min}–${item.repRange.max}",
                            onMinus = { vm.update(index) { it.copy(repRange = it.repRange.shift(-1)) } },
                            onPlus = { vm.update(index) { it.copy(repRange = it.repRange.shift(1)) } },
                        )
                    }
                    EditorRow(stringResource(R.string.routine_rest)) {
                        Stepper(
                            Formatting.duration(item.restSec.toLong()),
                            onMinus = { vm.update(index) { it.copy(restSec = (it.restSec - 15).coerceAtLeast(15)) } },
                            onPlus = { vm.update(index) { it.copy(restSec = (it.restSec + 15).coerceAtMost(600)) } },
                        )
                    }
                    if (index < routine.items.lastIndex || item.group != null) {
                        val linkedToNext = item.group != null && routine.items.getOrNull(index + 1)?.group == item.group
                        TextButton(onClick = { if (linkedToNext) vm.unlink(index + 1) else if (index < routine.items.lastIndex) vm.linkWithNext(index) else vm.unlink(index) }) {
                            Icon(if (linkedToNext) Icons.Rounded.LinkOff else Icons.Rounded.Link, contentDescription = null, tint = Knurl.colors.superset)
                            Text(
                                stringResource(if (linkedToNext) R.string.superset_unlink_next else if (index < routine.items.lastIndex) R.string.superset_link_next else R.string.superset_leave),
                                color = Knurl.colors.superset,
                            )
                        }
                    }
                } }
                // Supersets: the same bar as in the workout, instead of a frame around the card.
                if (item.group != null) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(Knurl.colors.superset, RoundedCornerShape(2.dp)))
                        card(Modifier.weight(1f))
                    }
                } else card(Modifier)
            }
        }
    }
    if (confirmDiscard) DiscardDialog(onDismiss = { confirmDiscard = false }, onDiscard = { confirmDiscard = false; go.back() })
}

@Composable
private fun DiscardDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) = ConfirmDialog(
    title = stringResource(R.string.routine_discard_title),
    body = stringResource(R.string.routine_discard_body),
    confirm = stringResource(R.string.action_discard_changes),
    onDismiss = onDismiss,
    onConfirm = onDiscard,
)

@Composable
private fun EditorRow(label: String, control: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.width(190.dp)) { control() }
    }
}

/** Moves the rep range by one step along common ranges (3–5, 5–8, 8–12, 12–15, 15–20). */
private fun RepRange.shift(by: Int): RepRange {
    val ranges = listOf(RepRange(1, 3), RepRange(3, 5), RepRange(5, 8), RepRange(6, 10), RepRange(8, 12), RepRange(10, 15), RepRange(12, 15), RepRange(15, 20))
    val index = ranges.indexOf(this).takeIf { it >= 0 } ?: ranges.indexOfFirst { it.min >= min }.coerceAtLeast(0)
    return ranges[(index + by).coerceIn(ranges.indices)]
}
