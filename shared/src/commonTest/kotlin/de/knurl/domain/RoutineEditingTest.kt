package de.knurl.domain

import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineEditingTest {

    private val routine = Routine("r", "Push", listOf(RoutineItem("a", 3), RoutineItem("b", 3), RoutineItem("c", 3)))

    @Test fun moveUpAndDown() {
        assertEquals(listOf("b", "a", "c"), RoutineEditing.move(routine, 1, -1).items.map { it.exerciseId })
        assertEquals(listOf("a", "c", "b"), RoutineEditing.move(routine, 1, 1).items.map { it.exerciseId })
    }

    @Test fun movingPastTheEndsChangesNothing() {
        assertEquals(routine, RoutineEditing.move(routine, 0, -1))
        assertEquals(routine, RoutineEditing.move(routine, 2, 1))
    }

    @Test fun routineFromAFreeWorkoutCountsWorkingSets() {
        val workout = Workout(
            id = "w", name = "Training", startedAtMs = 0, endedAtMs = 1,
            exercises = listOf(
                WorkoutExercise("bench_press", listOf(SetEntry(SetKind.WARMUP, 60.0, 10), SetEntry(weightKg = 100.0, reps = 5), SetEntry(weightKg = 100.0, reps = 5)), RepRange(5, 8), 180),
                WorkoutExercise("pec_deck", listOf(SetEntry(weightKg = 50.0, reps = 12))),
            ),
        )
        val made = RoutineEditing.fromWorkout("new", "Brust", workout, position = 4)
        assertEquals(listOf(2, 1), made.items.map { it.sets })
        assertEquals(RepRange(5, 8), made.items.first().repRange)
        assertEquals(180, made.items.first().restSec)
        assertEquals(4, made.position)
    }

    @Test fun nextPositionGoesToTheEnd() {
        assertEquals(0, RoutineEditing.nextPosition(emptyList()))
        assertEquals(4, RoutineEditing.nextPosition(listOf(routine.copy(position = 3))))
    }
}
