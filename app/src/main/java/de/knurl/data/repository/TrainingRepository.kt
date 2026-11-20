package de.knurl.data.repository

import android.util.Log
import de.knurl.data.Settings
import de.knurl.data.TrainingData
import de.knurl.domain.WorkoutPlanning
import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.catalog.StarterRoutines
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The single source of truth the screens read. It follows the signed-in user: on every account
 * change it opens that user's store, creates the profile on first sight and mirrors the data.
 *
 * The running workout is edited many times a minute, so it is applied here first and written
 * behind; the store only reports changes made elsewhere (another device).
 */
class TrainingRepository(
    private val store: TrainingStore,
    uid: Flow<String?>,
    private val scope: CoroutineScope,
    private val german: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val _data = MutableStateFlow<TrainingData?>(null)

    /** Null while no user is signed in or the first data has not arrived yet. */
    val data: StateFlow<TrainingData?> = _data.asStateFlow()

    private var current: UserTrainingStore? = null

    /** The running workout as this device knows it; local edits win over echoes. */
    private var active: Workout? = null

    init {
        scope.launch {
            uid.collectLatest { id ->
                current = null
                active = null
                _data.value = null
                if (id == null) return@collectLatest
                val userStore = store.open(id)
                current = userStore
                runCatching { userStore.ensureProfile(StarterRoutines.all(german()), Settings()) }
                    .onFailure { Log.w(TAG, "Could not create profile yet", it) }
                coroutineScope {
                    val firstActive = CompletableDeferred<Unit>()
                    launch {
                        userStore.activeChanges.collect { remote ->
                            active = remote
                            _data.update { it?.copy(active = remote) }
                            firstActive.complete(Unit)
                        }
                    }
                    firstActive.await()
                    // Other changes never carry the running workout: ours is newer than any echo.
                    userStore.data.collect { _data.value = it.copy(active = active) }
                }
            }
        }
    }

    fun exercise(id: String): Exercise? =
        ExerciseCatalog.find(id) ?: _data.value?.customExercises?.firstOrNull { it.id == id }

    fun allExercises(): List<Exercise> = ExerciseCatalog.all + _data.value?.customExercises.orEmpty()

    /** Starts a workout unless one is already running, in which case nothing changes. */
    fun startWorkout(routine: Routine?, freeName: String) {
        val data = _data.value ?: return
        if (data.active != null) return
        val id = newId()
        val workout = routine?.let { WorkoutPlanning.fromRoutine(id, it, now(), data.workouts, ::exercise) }
            ?: WorkoutPlanning.empty(id, freeName, now())
        setActive(workout)
    }

    fun updateActive(transform: (Workout) -> Workout) {
        val active = _data.value?.active ?: return
        setActive(transform(active))
    }

    fun addExerciseToActive(exerciseId: String) {
        val data = _data.value ?: return
        val active = data.active ?: return
        val planned = WorkoutPlanning.plannedExercise(
            exerciseId, sets = 3, range = RepRange.Hypertrophy, restSec = data.settings.defaultRestSec,
            history = data.workouts, lookup = ::exercise,
        )
        setActive(active.copy(exercises = active.exercises + planned))
    }

    /** Saves the ticked-off sets. A workout with nothing ticked off is simply dropped. */
    fun finishActive(): Workout? {
        val data = _data.value ?: return null
        val active = data.active ?: return null
        val saved = WorkoutPlanning.finish(active, now())
        this.active = null
        _data.value = data.copy(active = null, workouts = if (saved != null) data.workouts + saved else data.workouts)
        current?.finishActive(saved)
        return saved
    }

    fun discardActive() = setActive(null)

    fun deleteWorkout(id: String) {
        current?.deleteWorkout(id)
    }

    fun saveRoutine(routine: Routine) {
        current?.saveRoutine(routine)
    }

    fun deleteRoutine(id: String) {
        current?.deleteRoutine(id)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val data = _data.value ?: return
        val settings = transform(data.settings)
        _data.value = data.copy(settings = settings)
        current?.saveSettings(settings)
    }

    /** Deletes all of this user's data on the server. False if it could not (e.g. offline). */
    suspend fun deleteAllData(): Boolean {
        val store = current ?: return false
        return runCatching { store.deleteEverything() }
            .onFailure { Log.w(TAG, "Could not delete data", it) }
            .isSuccess
    }

    private fun setActive(workout: Workout?) {
        val data = _data.value ?: return
        active = workout
        _data.value = data.copy(active = workout)
        current?.saveActive(workout)
    }

    private companion object {
        const val TAG = "TrainingRepository"
    }
}
