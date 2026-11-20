package de.knurl.data.repository

import de.knurl.data.Settings
import de.knurl.data.TrainingData
import de.knurl.data.local.JsonFileStore
import de.knurl.domain.WorkoutPlanning
import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.catalog.StarterRoutines
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The single source of truth for training data. Local only for now: the JSON file in app storage
 * is the truth, and every change is written straight back. A sync layer can sit behind this later
 * without changing any screen.
 */
class TrainingRepository(
    private val store: JsonFileStore<TrainingData>,
    private val scope: CoroutineScope,
    private val german: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _data = MutableStateFlow<TrainingData?>(null)

    /** Null until the file has been read. */
    val data: StateFlow<TrainingData?> = _data.asStateFlow()

    init {
        scope.launch {
            _data.value = store.read() ?: TrainingData(routines = StarterRoutines.all(german())).also { store.write(it) }
        }
    }

    fun exercise(id: String): Exercise? =
        ExerciseCatalog.find(id) ?: _data.value?.customExercises?.firstOrNull { it.id == id }

    fun allExercises(): List<Exercise> = ExerciseCatalog.all + _data.value?.customExercises.orEmpty()

    /** Starts a workout unless one is already running, in which case nothing changes. */
    fun startWorkout(routine: Routine?, freeName: String) {
        mutate { data ->
            if (data.active != null) return@mutate data
            val id = UUID.randomUUID().toString()
            val workout = routine?.let { WorkoutPlanning.fromRoutine(id, it, now(), data.workouts, ::exercise) }
                ?: WorkoutPlanning.empty(id, freeName, now())
            data.copy(active = workout)
        }
    }

    fun updateActive(transform: (Workout) -> Workout) {
        mutate { data -> data.active?.let { data.copy(active = transform(it)) } ?: data }
    }

    fun addExerciseToActive(exerciseId: String) {
        mutate { data ->
            val active = data.active ?: return@mutate data
            val planned = WorkoutPlanning.plannedExercise(
                exerciseId, sets = 3, range = RepRange.Hypertrophy, restSec = data.settings.defaultRestSec,
                history = data.workouts, lookup = ::exercise,
            )
            data.copy(active = active.copy(exercises = active.exercises + planned))
        }
    }

    /** Saves the ticked-off sets. A workout with nothing ticked off is simply dropped. */
    fun finishActive() {
        mutate { data ->
            val active = data.active ?: return@mutate data
            val saved = WorkoutPlanning.finish(active, now())
            data.copy(active = null, workouts = if (saved != null) data.workouts + saved else data.workouts)
        }
    }

    fun discardActive() = mutate { it.copy(active = null) }

    fun deleteWorkout(id: String) = mutate { data -> data.copy(workouts = data.workouts.filterNot { it.id == id }) }

    fun updateSettings(transform: (Settings) -> Settings) = mutate { it.copy(settings = transform(it.settings)) }

    private fun mutate(transform: (TrainingData) -> TrainingData) {
        if (_data.value == null) return
        _data.update { current -> current?.let(transform) }
        // Always write the latest value: writes are serialized, so the last one to run wins with current data.
        scope.launch { _data.value?.let { store.write(it) } }
    }
}
