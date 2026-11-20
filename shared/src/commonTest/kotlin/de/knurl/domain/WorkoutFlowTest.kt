package de.knurl.domain

import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutFlowTest {

    private fun ex(id: String, sets: Int, group: Int? = null, done: Int = 0) =
        WorkoutExercise(id, List(sets) { SetEntry(weightKg = 10.0, reps = 10, done = it < done) }, group = group)

    private fun workout(vararg e: WorkoutExercise) = Workout(id = "w", name = "w", startedAtMs = 0, exercises = e.toList())

    @Test fun singleExercisesGoSetBySet() {
        val w = workout(ex("a", 2), ex("b", 2))
        assertEquals(listOf(SetRef(0, 0), SetRef(0, 1), SetRef(1, 0), SetRef(1, 1)), WorkoutFlow.order(w))
    }

    @Test fun supersetsAlternateInRounds() {
        val w = workout(ex("a", 2, group = 1), ex("b", 3, group = 1), ex("c", 1))
        assertEquals(
            listOf(SetRef(0, 0), SetRef(1, 0), SetRef(0, 1), SetRef(1, 1), SetRef(1, 2), SetRef(2, 0)),
            WorkoutFlow.order(w),
        )
    }

    @Test fun nextFocusSkipsDoneSetsAndWraps() {
        val w = workout(ex("a", 2, done = 2), ex("b", 2, done = 1))
        assertEquals(SetRef(1, 1), WorkoutFlow.nextFocus(w, SetRef(1, 0)))
        // nothing open after the last set: wrap to an earlier open one
        val w2 = workout(ex("a", 2, done = 1), ex("b", 1, done = 1))
        assertEquals(SetRef(0, 1), WorkoutFlow.nextFocus(w2, SetRef(1, 0)))
        assertNull(WorkoutFlow.nextFocus(workout(ex("a", 1, done = 1)), null))
    }

    @Test fun restOnlyAfterTheLastExerciseOfASupersetRound() {
        val w = workout(ex("a", 2, group = 1), ex("b", 2, group = 1), ex("c", 2))
        assertFalse(WorkoutFlow.restsAfter(w, SetRef(0, 0)))
        assertTrue(WorkoutFlow.restsAfter(w, SetRef(1, 0)))
        assertTrue(WorkoutFlow.restsAfter(w, SetRef(2, 1)))
    }

    @Test fun linkingAndUnlinking() {
        assertEquals(listOf(1, 1, null), Supersets.link(listOf(null, null, null), 0))
        assertEquals(listOf(1, 1, 1), Supersets.link(listOf(1, 1, null), 1))
        assertEquals(listOf(null, null, null), Supersets.unlink(listOf(1, 1, null), 0))
        assertEquals(listOf(1, 1, null, 2, 2), Supersets.tidy(listOf(7, 7, null, 7, 7)))
    }

    @Test fun upNextCarriesOnAfterTheFurthestSetDone() {
        fun ex(id: String, vararg done: Boolean) = WorkoutExercise(id, done.map { SetEntry(weightKg = 50.0, reps = 8, done = it) })
        val workout = Workout(
            id = "w", name = "w", startedAtMs = 0,
            exercises = listOf(ex("bench_press", true, false), ex("barbell_row", false, false), ex("squat", true, false)),
        )
        // Skipped ahead to squats: carry on there, the bench set comes after.
        assertEquals(SetRef(2, 1), WorkoutFlow.upNext(workout))
        assertEquals(SetRef(0, 0), WorkoutFlow.upNext(workout.copy(exercises = listOf(ex("bench_press", false, false)))))
        assertNull(WorkoutFlow.upNext(workout.copy(exercises = listOf(ex("bench_press", true)))))
    }
}
