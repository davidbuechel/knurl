package de.knurl.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.core.workout.RestTimer
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.SetRef
import de.knurl.domain.WorkoutFlow
import de.knurl.domain.WorkoutPlanning
import de.knurl.domain.linkWithNext
import de.knurl.domain.unlink
import de.knurl.domain.addSet
import de.knurl.domain.calc.Warmup
import de.knurl.domain.prependWarmups
import de.knurl.domain.setExerciseNote
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Progression
import de.knurl.domain.changeWeight
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.RepRange
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
import kotlinx.coroutines.flow.update

data class ExerciseCard(
    val exercise: Exercise?,
    val exerciseId: String,
    val sets: List<SetEntry>,
    val lastTime: List<SetEntry>,
    val suggestion: Progression.Suggestion?,
    val weightStepKg: Double,
    val note: String,
    /** Superset group of this exercise, or null. */
    val group: Int?,
)

/** The cell being edited with the number pad. */
enum class Field { WEIGHT, REPS }

data class Focus(val ref: SetRef, val field: Field)

data class ActiveWorkoutState(
    /** Null once the workout has been finished or discarded; the screen then closes. */
    val workout: Workout?,
    val cards: List<ExerciseCard>,
    val elapsedSec: Long,
    /** Seconds left on the rest timer, or null when no rest is running. */
    val restLeftSec: Long?,
    /** Editing a finished workout from the history instead of logging the running one. */
    val editing: Boolean,
    /** Editing a past workout and something differs from how it was opened. */
    val changed: Boolean = false,
    val focus: Focus?,
    /** Length of the running rest, for its progress. */
    val restTotalSec: Long,
    val doneSets: Int,
    val totalSets: Int,
)

/**
 * The set list. Works on the running workout, or with [editWorkoutId] on a copy of a finished
 * workout that is saved on [finish]. Same screen, same controls.
 */
class ActiveWorkoutViewModel(
    private val training: TrainingRepository,
    private val rest: RestTimer,
    private val editWorkoutId: String? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val editing = MutableStateFlow(editWorkoutId?.let { id -> training.workout(id)?.markAllDone() })

    /** The past workout as it was opened, to tell whether leaving would lose changes. */
    private val original = editing.value

    /** Where the number pad points. Starts on the first open set of a running workout. */
    private val focus = MutableStateFlow<Focus?>(null)

    private val ticker = flow {
        while (true) {
            emit(now())
            delay(1_000)
        }
    }

    val state: StateFlow<ActiveWorkoutState?> =
        combine(training.data.filterNotNull(), editing, rest.endsAtMs, ticker, focus) { data, edited, restEnd, nowMs, focused ->
            val workout = if (editWorkoutId != null) edited else data.active
            val (done, total) = workout?.let(WorkoutFlow::progress) ?: (0 to 0)
            // A focus that points past the end (set removed) is dropped.
            val validFocus = focused?.takeIf { f -> workout?.exercises?.getOrNull(f.ref.exercise)?.sets?.getOrNull(f.ref.set) != null }
            val history = data.workouts.filter { it.id != workout?.id }
            ActiveWorkoutState(
                workout = workout,
                cards = workout?.exercises.orEmpty().map { item ->
                    val exercise = training.exercise(item.exerciseId)
                    val last = History.lastSets(item.exerciseId, history)
                    ExerciseCard(
                        exercise = exercise,
                        exerciseId = item.exerciseId,
                        sets = item.sets,
                        lastTime = last.filter { it.kind.counts },
                        suggestion = exercise?.let { Progression.suggest(last, item.repRange, it.equipment) },
                        weightStepKg = exercise?.let { Progression.increment(it.equipment) }?.takeIf { it > 0 } ?: 2.5,
                        note = item.note,
                        group = item.group,
                    )
                },
                elapsedSec = workout?.let { ((it.endedAtMs ?: nowMs) - it.startedAtMs) / 1000 } ?: 0,
                restLeftSec = if (editWorkoutId != null) null else restEnd?.let { ((it - nowMs) / 1000).takeIf { left -> left >= 0 } },
                editing = editWorkoutId != null,
                changed = editWorkoutId != null && edited != original,
                focus = validFocus,
                restTotalSec = rest.totalMs.value / 1000,
                doneSets = done,
                totalSets = total,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(transform: (Workout) -> Workout) {
        if (editWorkoutId != null) editing.update { it?.let(transform) } else training.updateActive(transform)
    }

    private fun current(): Workout? = if (editWorkoutId != null) editing.value else training.data.value?.active

    fun changeWeight(exercise: Int, set: Int, deltaKg: Double) = edit { w ->
        val currentKg = w.exercises.getOrNull(exercise)?.sets?.getOrNull(set)?.weightKg ?: return@edit w
        w.changeWeight(exercise, set, (currentKg + deltaKg).coerceAtLeast(0.0))
    }

    fun setWeight(exercise: Int, set: Int, kg: Double) = edit { it.changeWeight(exercise, set, kg) }

    fun changeReps(exercise: Int, set: Int, delta: Int) =
        edit { w -> w.updateSet(exercise, set) { it.copy(reps = (it.reps + delta).coerceAtLeast(0)) } }

    fun setReps(exercise: Int, set: Int, reps: Int) = edit { w -> w.updateSet(exercise, set) { it.copy(reps = reps) } }

    /**
     * Ticking a set off: starts the rest (after the last exercise of a superset round, not when
     * editing the history) and moves the focus to the next open set in workout order.
     */
    fun toggleDone(exercise: Int, set: Int) {
        val workout = current() ?: return
        val item = workout.exercises.getOrNull(exercise) ?: return
        val nowDone = item.sets.getOrNull(set)?.done?.not() ?: return
        val ref = SetRef(exercise, set)
        edit { w -> w.updateSet(exercise, set) { it.copy(done = nowDone) } }
        if (editWorkoutId == null) {
            if (nowDone && WorkoutFlow.restsAfter(workout, ref)) rest.start(item.restSec) else if (!nowDone) rest.stop()
        }
        if (nowDone) {
            val after = current() ?: return
            focus.value = WorkoutFlow.nextFocus(after, ref)?.let { Focus(it, Field.WEIGHT) }
        }
    }

    fun focus(exercise: Int, set: Int, field: Field) {
        focus.value = Focus(SetRef(exercise, set), field)
    }

    fun clearFocus() {
        focus.value = null
    }

    /** "Next" on the pad: weight → reps of the same set; reps → tick the set and go on. */
    fun next() {
        val f = focus.value ?: return
        if (f.field == Field.WEIGHT) focus.value = f.copy(field = Field.REPS)
        else {
            val set = current()?.exercises?.getOrNull(f.ref.exercise)?.sets?.getOrNull(f.ref.set) ?: return
            if (!set.done) toggleDone(f.ref.exercise, f.ref.set)
            else focus.value = current()?.let { WorkoutFlow.nextFocus(it, f.ref) }?.let { Focus(it, Field.WEIGHT) }
        }
    }

    fun linkWithNext(exercise: Int) = edit { it.linkWithNext(exercise) }

    fun unlink(exercise: Int) = edit { it.unlink(exercise) }

    /** Working → warm-up → drop set → working. */
    fun cycleKind(exercise: Int, set: Int) = edit { w ->
        w.updateSet(exercise, set) {
            it.copy(kind = when (it.kind) {
                SetKind.WORKING -> SetKind.WARMUP
                SetKind.WARMUP -> SetKind.DROP
                SetKind.DROP, SetKind.FAILURE -> SetKind.WORKING
            })
        }
    }

    fun addSet(exercise: Int) = edit { it.addSet(exercise) }

    /** None → 3 → 2 → 1 → 0 → none: reps left in the tank. */
    fun cycleRir(exercise: Int, set: Int) = edit { w ->
        w.updateSet(exercise, set) { it.copy(rir = it.rir.let { r -> when (r) { null -> 3; 0 -> null; else -> r - 1 } }) }
    }

    fun addWarmups(exercise: Int) = edit { w ->
        val item = w.exercises.getOrNull(exercise) ?: return@edit w
        val working = item.sets.firstOrNull { it.kind != SetKind.WARMUP }?.weightKg ?: return@edit w
        val equipment = training.exercise(item.exerciseId)?.equipment ?: return@edit w
        w.prependWarmups(exercise, Warmup.sets(working, equipment, training.data.value?.settings?.barKg ?: 20.0))
    }

    fun setExerciseNote(exercise: Int, note: String) = edit { it.setExerciseNote(exercise, note) }

    fun setWorkoutNote(note: String) = edit { it.copy(note = note.take(2000)) }

    fun removeSet(exercise: Int, set: Int) {
        if (focus.value?.ref == SetRef(exercise, set)) focus.value = null
        edit { it.removeSet(exercise, set) }
    }

    /** Indexes after it shift, so the number pad closes rather than point at another exercise. */
    fun removeExercise(exercise: Int) {
        focus.value = null
        edit { it.removeExercise(exercise) }
    }

    /** An exercise picked in the library. */
    fun addExercise(exerciseId: String) {
        if (editWorkoutId == null) return training.addExerciseToActive(exerciseId)
        val data = training.data.value ?: return
        val planned = WorkoutPlanning.plannedExercise(exerciseId, 3, RepRange.Hypertrophy, data.settings.defaultRestSec, data.workouts, training::exercise)
        editing.update { it?.copy(exercises = it.exercises + planned) }
    }

    fun addRest(seconds: Int) = rest.add(seconds)

    fun skipRest() = rest.stop()

    /** Called once when the countdown reaches zero on screen. */
    fun restFinished() = rest.clearIfOver()

    /** Ends the workout (or saves the edit). Returns the id of the saved workout, null if nothing was saved. */
    fun finish(): String? {
        if (editWorkoutId != null) {
            val edited = editing.value ?: return null
            val saved = WorkoutPlanning.finish(edited, edited.endedAtMs ?: now())
            if (saved != null) training.saveWorkout(saved) else training.deleteWorkout(edited.id)
            editing.value = null
            return saved?.id
        }
        rest.stop()
        return training.finishActive()?.id
    }

    fun discard() {
        if (editWorkoutId != null) {
            editing.value = null
            return
        }
        rest.stop()
        training.discardActive()
    }

    /** Saved sets are all done; marking them keeps them when the edit is saved. */
    private fun Workout.markAllDone() = copy(exercises = exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = true) }) })
}
