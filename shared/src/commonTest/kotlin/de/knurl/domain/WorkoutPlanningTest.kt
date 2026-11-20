package de.knurl.domain

import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkoutPlanningTest {

    private val lastBench = Workout(
        id = "old",
        name = "Push",
        startedAtMs = 0,
        endedAtMs = 1,
        exercises = listOf(WorkoutExercise("bench_press", List(3) { SetEntry(weightKg = 97.5, reps = 8, done = true) })),
    )

    @Test fun routinePrefillsTheSuggestion() {
        val routine = Routine("r", "Push", listOf(RoutineItem("bench_press", 3, RepRange(5, 8))))
        val workout = WorkoutPlanning.fromRoutine("new", routine, 10, listOf(lastBench), ExerciseCatalog::find)
        val sets = workout.exercises.single().sets
        assertEquals(3, sets.size)
        assertEquals(100.0, sets.first().weightKg)
        assertEquals(5, sets.first().reps)
    }

    @Test fun finishKeepsOnlyTickedSets() {
        val workout = Workout(
            id = "w", name = "w", startedAtMs = 0,
            exercises = listOf(
                WorkoutExercise("bench_press", listOf(SetEntry(weightKg = 100.0, reps = 5, done = true), SetEntry(weightKg = 100.0, reps = 5))),
                WorkoutExercise("pec_deck", listOf(SetEntry(weightKg = 50.0, reps = 12))),
            ),
        )
        val saved = WorkoutPlanning.finish(workout, 99)!!
        assertEquals(1, saved.exercises.size)
        assertEquals(1, saved.exercises.single().sets.size)
        assertEquals(99L, saved.endedAtMs)
    }

    @Test fun nothingTickedMeansNothingToSave() =
        assertNull(WorkoutPlanning.finish(WorkoutPlanning.empty("w", "w", 0), 1))

    @Test fun weightChangeCarriesToOpenSetsWithTheSameWeight() {
        val workout = Workout(
            id = "w", name = "w", startedAtMs = 0,
            exercises = listOf(WorkoutExercise("bench_press", listOf(
                SetEntry(weightKg = 0.0, reps = 5),
                SetEntry(weightKg = 0.0, reps = 5),
                SetEntry(weightKg = 0.0, reps = 5, done = true),
                SetEntry(weightKg = 60.0, reps = 5),
            ))),
        )
        val weights = workout.changeWeight(0, 0, 100.0).exercises.single().sets.map { it.weightKg }
        assertEquals(listOf(100.0, 100.0, 0.0, 60.0), weights)
    }

    @Test fun addSetCopiesTheLastSetUnticked() {
        val workout = lastBench.addSet(0)
        assertEquals(4, workout.exercises.single().sets.size)
        assertEquals(false, workout.exercises.single().sets.last().done)
    }

    @Test fun removingAnExerciseDissolvesASupersetOfOne() {
        val workout = Workout(
            id = "w", name = "w", startedAtMs = 0,
            exercises = listOf(
                WorkoutExercise("bench_press", emptyList(), group = 1),
                WorkoutExercise("barbell_row", emptyList(), group = 1),
                WorkoutExercise("squat", emptyList()),
            ),
        )
        assertEquals(listOf<Int?>(null, null), workout.removeExercise(1).exercises.map { it.group })
    }
}
