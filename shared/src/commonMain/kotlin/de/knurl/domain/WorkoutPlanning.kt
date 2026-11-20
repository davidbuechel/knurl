package de.knurl.domain

import de.knurl.domain.calc.ExerciseLookup
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Progression
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise

/** Builds and edits workouts. Pure functions, so the app only stores what comes out. */
object WorkoutPlanning {

    /** A new workout from a routine, every set pre-filled with the suggestion from the last session. */
    fun fromRoutine(id: String, routine: Routine, nowMs: Long, history: List<Workout>, lookup: ExerciseLookup) = Workout(
        id = id,
        name = routine.name,
        routineId = routine.id,
        startedAtMs = nowMs,
        exercises = routine.items.map { item ->
            plannedExercise(item.exerciseId, item.sets, item.repRange, item.restSec, history, lookup).copy(group = item.group)
        },
    )

    fun empty(id: String, name: String, nowMs: Long) = Workout(id = id, name = name, startedAtMs = nowMs)

    fun plannedExercise(
        exerciseId: String,
        sets: Int,
        range: RepRange,
        restSec: Int,
        history: List<Workout>,
        lookup: ExerciseLookup,
    ): WorkoutExercise {
        val last = History.lastSets(exerciseId, history)
        val equipment = lookup(exerciseId)?.equipment
        val suggestion = equipment?.let { Progression.suggest(last, range, it) }
        val set = SetEntry(
            kind = SetKind.WORKING,
            weightKg = suggestion?.weightKg ?: 0.0,
            reps = suggestion?.reps ?: range.min,
        )
        return WorkoutExercise(exerciseId, List(sets.coerceAtLeast(1)) { set }, range, restSec)
    }

    /** The workout as it gets saved: only sets that were ticked off, and no empty exercises. */
    fun finish(workout: Workout, nowMs: Long): Workout? {
        val done = workout.exercises
            .map { it.copy(sets = it.sets.filter(SetEntry::done)) }
            .filter { it.sets.isNotEmpty() }
        return if (done.isEmpty()) null else workout.copy(exercises = done, endedAtMs = nowMs)
    }
}

fun Workout.updateSet(exercise: Int, set: Int, transform: (SetEntry) -> SetEntry): Workout =
    updateExercise(exercise) { item ->
        item.copy(sets = item.sets.mapIndexed { i, s -> if (i == set) transform(s) else s })
    }

/**
 * Sets a set's weight and carries it to the following sets that are not ticked off yet and had the
 * same weight, so changing set 1 doesn't mean changing every set by hand.
 */
fun Workout.changeWeight(exercise: Int, set: Int, weightKg: Double): Workout = updateExercise(exercise) { item ->
    val before = item.sets.getOrNull(set)?.weightKg ?: return@updateExercise item
    item.copy(sets = item.sets.mapIndexed { i, s ->
        when {
            i == set -> s.copy(weightKg = weightKg)
            i > set && !s.done && s.weightKg == before -> s.copy(weightKg = weightKg)
            else -> s
        }
    })
}

/** Adds a set that copies the last one, not yet ticked off. */
fun Workout.addSet(exercise: Int): Workout = updateExercise(exercise) { item ->
    val template = item.sets.lastOrNull()?.copy(done = false, kind = SetKind.WORKING)
        ?: SetEntry(weightKg = 0.0, reps = item.repRange.min)
    item.copy(sets = item.sets + template)
}

fun Workout.removeSet(exercise: Int, set: Int): Workout =
    updateExercise(exercise) { item -> item.copy(sets = item.sets.filterIndexed { i, _ -> i != set }) }

fun Workout.setExerciseNote(exercise: Int, note: String): Workout = updateExercise(exercise) { it.copy(note = note.take(500)) }

/** Puts warm-up sets in front of the exercise's sets, unless it already has warm-ups. */
fun Workout.prependWarmups(exercise: Int, warmups: List<SetEntry>): Workout = updateExercise(exercise) { item ->
    if (warmups.isEmpty() || item.sets.any { it.kind == SetKind.WARMUP }) item else item.copy(sets = warmups + item.sets)
}

fun Workout.removeExercise(exercise: Int): Workout =
    copy(exercises = exercises.filterIndexed { i, _ -> i != exercise })

private fun Workout.updateExercise(index: Int, transform: (WorkoutExercise) -> WorkoutExercise): Workout =
    copy(exercises = exercises.mapIndexed { i, e -> if (i == index) transform(e) else e })
