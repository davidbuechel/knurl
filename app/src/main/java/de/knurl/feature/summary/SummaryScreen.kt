package de.knurl.feature.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import de.knurl.feature.exercises.ExerciseThumbnail
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import de.knurl.R
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.PersonalRecord
import de.knurl.domain.calc.Volume
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.NameDialog
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.setText
import de.knurl.feature.common.tonnageText
import de.knurl.feature.common.weightText
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.theme.Knurl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class SummaryState(
    val workout: Workout,
    val durationSec: Long,
    val sets: Int,
    val volumeKg: Double,
    /** Volume of the previous session of the same routine (or name), to compare. */
    val previousVolumeKg: Double?,
    val records: List<PersonalRecord>,
    /** The routine this workout came from, if it still exists. */
    val routine: Routine?,
    val savedAsRoutine: Boolean,
)

class SummaryViewModel(private val training: TrainingRepository, private val workoutId: String) : ViewModel() {

    private val saved = MutableStateFlow(false)

    val state: StateFlow<SummaryState?> = combine(training.data.filterNotNull(), saved) { data, saved ->
        val workout = data.workouts.firstOrNull { it.id == workoutId } ?: return@combine null
        val previous = data.workouts
            .filter { it.id != workout.id && it.startedAtMs < workout.startedAtMs }
            .filter { if (workout.routineId != null) it.routineId == workout.routineId else it.name == workout.name }
            .maxByOrNull { it.startedAtMs }
        SummaryState(
            workout = workout,
            durationSec = ((workout.endedAtMs ?: workout.startedAtMs) - workout.startedAtMs) / 1000,
            sets = workout.exercises.sumOf { it.sets.size },
            volumeKg = Volume.of(workout),
            previousVolumeKg = previous?.let(Volume::of),
            records = History.records(data.workouts).filter { it.workoutId == workout.id },
            routine = workout.routineId?.let { id -> data.routines.firstOrNull { it.id == id } },
            savedAsRoutine = saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exercise(id: String) = training.exercise(id)

    fun exerciseName(id: String, german: Boolean) = training.exercise(id)?.name(german) ?: id

    fun saveAsRoutine(name: String) {
        val workout = state.value?.workout ?: return
        training.saveAsRoutine(workout, name)
        saved.value = true
    }

    /** The routine takes over what was done today: exercises, order, number of sets. */
    fun updateRoutine() {
        val s = state.value ?: return
        val routine = s.routine ?: return
        training.saveRoutine(de.knurl.domain.RoutineEditing.updateFromWorkout(routine, s.workout))
        saved.value = true
    }
}

/** After a workout: what was done, what improved, and whether to keep it as a routine. */
@Composable
fun SummaryScreen(go: Navigator, workoutId: String) {
    val vm = knurlViewModel(key = "summary-$workoutId") { SummaryViewModel(it.training, workoutId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var naming by rememberSaveable { mutableStateOf(false) }
    val german = isGerman()

    Scaffold(
        bottomBar = {
            Button(
                onClick = go::back,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.action_done)) }
        },
    ) { padding ->
        val s = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenTitle(stringResource(R.string.summary_title), eyebrow = s.workout.name) }

            item {
                KpiRow(
                    listOf(
                        Kpi(Formatting.duration(s.durationSec), stringResource(R.string.summary_duration)),
                        Kpi("${s.sets}", stringResource(R.string.summary_sets)),
                        Kpi(tonnageText(s.volumeKg), stringResource(R.string.stat_volume)),
                    ),
                )
            }
            s.previousVolumeKg?.takeIf { it > 0 }?.let { previous ->
                item {
                    val change = ((s.volumeKg - previous) / previous * 100).toInt()
                    Text(
                        stringResource(if (change >= 0) R.string.summary_more_volume else R.string.summary_less_volume, kotlin.math.abs(change)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (change >= 0) Knurl.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            if (s.records.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.summary_records)) }
                item {
                    ListGroup {
                        s.records.forEachIndexed { i, record ->
                            ListRow(
                                vm.exerciseName(record.exerciseId, german),
                                subtitle = setText(record.set.weightKg, record.set.reps),
                                leading = { vm.exercise(record.exerciseId)?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                                trailing = { Text(stringResource(R.string.unit_e1rm, weightText(record.oneRepMaxKg)), style = MaterialTheme.typography.labelLarge) },
                                divider = i < s.records.lastIndex,
                            )
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.summary_routine)) }
            item {
                KnurlCard {
                    when {
                        s.savedAsRoutine -> Text(stringResource(R.string.summary_routine_saved), color = Knurl.colors.success)
                        s.routine != null -> {
                            Text(stringResource(R.string.summary_update_hint, s.routine.name), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = vm::updateRoutine) { Text(stringResource(R.string.summary_update_routine)) }
                        }
                        else -> {
                            Text(stringResource(R.string.summary_save_hint), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { naming = true }) { Text(stringResource(R.string.summary_save_routine)) }
                        }
                    }
                }
            }
        }

        if (naming) {
            NameDialog(
                title = stringResource(R.string.summary_save_routine),
                initial = s.workout.name,
                onDismiss = { naming = false },
                onConfirm = { vm.saveAsRoutine(it); naming = false },
            )
        }
    }
}
