package de.knurl.domain.calc

import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaloriesTest {
    private val minute = 60_000L
    private fun workout(minutes: Long, vararg exercises: Pair<String, Int>) = Workout(
        "w", "W", null, 0, minutes * minute,
        exercises.map { (id, sets) -> WorkoutExercise(id, List(sets) { SetEntry(weightKg = 60.0, reps = 10, done = true) }) },
    )

    private val weight = listOf(BodyWeight("b", 0, 80.0))

    @Test fun relaxedStrengthIsLight() {
        // 6 sets in 60 minutes: 0.1 sets a minute.
        val w = workout(60, "bench_press" to 6)
        assertEquals(Calories.MET_LIGHT, Calories.met(w), 0.001)
        assertEquals(60, Calories.activeMinutes(w).toInt())
        // Left running for five hours: capped at ten minutes per set.
        assertEquals(60, Calories.activeMinutes(workout(300, "bench_press" to 6)).toInt())
    }

    @Test fun denseStrengthIsHeavier() {
        val w = workout(60, "bench_press" to 24)
        assertEquals(Calories.MET_DENSE, Calories.met(w), 0.001)
        assertEquals(480, Calories.estimate(w, weight)) // 6 × 80 kg × 1 h
    }

    @Test fun conditioningRaisesIt() {
        val w = workout(40, "bench_press" to 8, "wall_ball" to 8)
        assertTrue(Calories.met(w) > Calories.met(workout(40, "bench_press" to 16)))
    }

    @Test fun noBodyWeightNoNumber() {
        assertNull(Calories.estimate(workout(60, "bench_press" to 20), emptyList()))
        // A weight from a year ago is too old.
        val old = listOf(BodyWeight("b", -365L * 24 * 60 * minute, 80.0))
        assertNull(Calories.estimate(workout(60, "bench_press" to 20), old))
    }

    @Test fun usesTheLatestWeightBeforeOrTheFirstAfter() {
        val weights = listOf(BodyWeight("a", -10 * minute, 82.0), BodyWeight("b", 10 * minute, 81.0))
        assertEquals(82.0, Calories.bodyWeightAt(0, weights))
        assertEquals(81.0, Calories.bodyWeightAt(20 * minute, weights))
        assertEquals(82.0, Calories.bodyWeightAt(-20 * minute, weights)) // nothing before: the first after
    }

    @Test fun heartRateNeedsAgeAndSex() {
        assertNull(Calories.fromHeartRate(130.0, 60.0, 80.0, null, true))
        val kcal = Calories.fromHeartRate(130.0, 60.0, 80.0, 35, true)!!
        assertTrue(kcal in 500..800, "was $kcal")
    }
}
