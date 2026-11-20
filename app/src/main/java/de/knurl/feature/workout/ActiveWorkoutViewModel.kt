package de.knurl.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.addSet
import de.knurl.domain.changeWeight
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Progression
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.removeExercise
import de.knurl.domain.removeSet
import de.knurl.domain.updateSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ExerciseCard(
    val exercise: Exercise?,
    val exerciseId: String,
    val sets: List<SetEntry>,
    val lastTime: List<SetEntry>,
    val suggestion: Progression.Suggestion?,
    val weightStepKg: Double,
)

data class ActiveWorkoutState(
    /** Null once the workout has been finished or discarded; the screen then closes. */
    val workout: Workout?,
    val cards: List<ExerciseCard>,
    val elapsedSec: Long,
    /** Seconds left on the rest timer, or null when no rest is running. */
    val restLeftSec: Long?,
)

class ActiveWorkoutViewModel(
    private val training: TrainingRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val restEndsAtMs = MutableStateFlow<Long?>(null)
    private val ticker = flow {
        while (true) {
            emit(now())
            delay(1_000)
        }
    }

    val state: StateFlow<ActiveWorkoutState?> = combine(training.data.filterNotNull(), restEndsAtMs, ticker) { data, restEnd, nowMs ->
        val workout = data.active
        ActiveWorkoutState(
            workout = workout,
            cards = workout?.exercises.orEmpty().map { item ->
                val exercise = training.exercise(item.exerciseId)
                val last = History.lastSets(item.exerciseId, data.workouts)
                ExerciseCard(
                    exercise = exercise,
                    exerciseId = item.exerciseId,
                    sets = item.sets,
                    lastTime = last.filter { it.kind.counts },
                    suggestion = exercise?.let { Progression.suggest(last, item.repRange, it.equipment) },
                    weightStepKg = exercise?.let { Progression.increment(it.equipment) }?.takeIf { it > 0 } ?: 2.5,
                )
            },
            elapsedSec = workout?.let { (nowMs - it.startedAtMs) / 1000 } ?: 0,
            restLeftSec = restEnd?.let { ((it - nowMs) / 1000).takeIf { left -> left >= 0 } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changeWeight(exercise: Int, set: Int, deltaKg: Double) = training.updateActive { w ->
        val current = w.exercises.getOrNull(exercise)?.sets?.getOrNull(set)?.weightKg ?: return@updateActive w
        w.changeWeight(exercise, set, (current + deltaKg).coerceAtLeast(0.0))
    }

    fun setWeight(exercise: Int, set: Int, kg: Double) = training.updateActive { it.changeWeight(exercise, set, kg) }

    fun changeReps(exercise: Int, set: Int, delta: Int) =
        training.updateActive { w -> w.updateSet(exercise, set) { it.copy(reps = (it.reps + delta).coerceAtLeast(0)) } }

    fun setReps(exercise: Int, set: Int, reps: Int) =
        training.updateActive { w -> w.updateSet(exercise, set) { it.copy(reps = reps) } }

    /** Ticking a set off starts the rest timer for that exercise. */
    fun toggleDone(exercise: Int, set: Int) {
        val workout = training.data.value?.active ?: return
        val item = workout.exercises.getOrNull(exercise) ?: return
        val nowDone = item.sets.getOrNull(set)?.done?.not() ?: return
        training.updateActive { w -> w.updateSet(exercise, set) { it.copy(done = nowDone) } }
        restEndsAtMs.value = if (nowDone) now() + item.restSec * 1000L else null
    }

    /** Working → warm-up → drop set → working. */
    fun cycleKind(exercise: Int, set: Int) = training.updateActive { w ->
        w.updateSet(exercise, set) {
            it.copy(kind = when (it.kind) {
                SetKind.WORKING -> SetKind.WARMUP
                SetKind.WARMUP -> SetKind.DROP
                SetKind.DROP, SetKind.FAILURE -> SetKind.WORKING
            })
        }
    }

    fun addSet(exercise: Int) = training.updateActive { it.addSet(exercise) }

    fun removeSet(exercise: Int, set: Int) = training.updateActive { it.removeSet(exercise, set) }

    fun removeExercise(exercise: Int) = training.updateActive { it.removeExercise(exercise) }

    fun addRest(seconds: Int) {
        restEndsAtMs.value = restEndsAtMs.value?.plus(seconds * 1000L)
    }

    fun skipRest() {
        restEndsAtMs.value = null
    }

    fun finish() = training.finishActive()

    fun discard() = training.discardActive()
}
