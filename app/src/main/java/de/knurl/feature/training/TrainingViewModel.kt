package de.knurl.feature.training

import de.knurl.domain.calc.Calories
import androidx.lifecycle.ViewModel
import de.knurl.core.workout.RestTimer
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Volume
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryRow(
    val workout: Workout,
    val durationSec: Long,
    val sets: Int,
    val volumeKg: Double,
    /** New records set in this workout. */
    val records: Int,
    /** Estimated energy, null without a body weight. */
    val kcal: Int?,
)

data class TrainingState(
    val hasActive: Boolean,
    /** Name of the workout that is running, if one is. */
    val activeName: String? = null,
    val routines: List<Routine>,
    val history: List<HistoryRow>,
    /** The routine "up next": the one after the routine trained last. */
    val nextRoutineId: String?,
)

class TrainingViewModel(private val training: TrainingRepository, private val rest: RestTimer) : ViewModel() {

    val state: StateFlow<TrainingState?> = training.data.filterNotNull()
        .map { data ->
            val records = History.records(data.workouts).groupingBy { it.workoutId }.eachCount()
            val usable = data.routines.filter { it.items.isNotEmpty() }
            val lastId = data.workouts.filter { it.routineId != null }.maxByOrNull { it.startedAtMs }?.routineId
            TrainingState(
                hasActive = data.active != null,
                activeName = data.active?.name,
                routines = data.routines,
                history = data.workouts.sortedByDescending { it.startedAtMs }.map { w ->
                    HistoryRow(
                        workout = w,
                        durationSec = ((w.endedAtMs ?: w.startedAtMs) - w.startedAtMs) / 1000,
                        sets = w.exercises.sumOf { it.sets.size },
                        volumeKg = Volume.of(w),
                        records = records[w.id] ?: 0,
                        kcal = Calories.estimate(w, data.bodyWeights),
                    )
                },
                nextRoutineId = usable.getOrNull((usable.indexOfFirst { it.id == lastId } + 1) % usable.size.coerceAtLeast(1))?.id,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun start(routine: Routine?, freeName: String) = training.startWorkout(routine, freeName)

    /** Throws the running workout away and starts [routine] instead. */
    fun replaceActive(routine: Routine, freeName: String) {
        rest.stop()
        training.discardActive()
        training.startWorkout(routine, freeName)
    }

    fun deleteWorkout(workout: Workout) = training.deleteWorkout(workout.id)

    fun restoreWorkout(workout: Workout) = training.saveWorkout(workout)

    fun saveAsRoutine(workout: Workout, name: String) = training.saveAsRoutine(workout, name)

    fun deleteRoutine(routine: Routine) = training.deleteRoutine(routine.id)

    fun restoreRoutine(routine: Routine) = training.saveRoutine(routine)

    fun duplicateRoutine(routine: Routine, name: String) = training.duplicateRoutine(routine, name)

    fun moveRoutine(routine: Routine, by: Int) = training.moveRoutine(routine.id, by)

    fun exercise(id: String) = training.exercise(id)

    fun exerciseName(id: String, german: Boolean) = training.exercise(id)?.name(german) ?: id
}
