package de.knurl.domain.achievements

import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val utc = TimeZone.UTC
private const val HOUR = 3_600_000L

private fun at(date: LocalDate, hour: Int = 18) = date.atStartOfDayIn(utc).toEpochMilliseconds() + hour * HOUR

private fun workout(id: String, date: LocalDate, vararg exercises: WorkoutExercise, hour: Int = 18) =
    Workout(id, id, null, at(date, hour), at(date, hour) + HOUR, exercises.toList())

private fun lift(id: String, kg: Double, reps: Int, sets: Int = 1, group: Int? = null) =
    WorkoutExercise(id, List(sets) { SetEntry(weightKg = kg, reps = reps, done = true) }, group = group)

class AchievementsTest {
    private val monday = LocalDate(2026, 11, 2)

    private fun status(list: List<AchievementStatus>, id: String) = list.first { it.achievement.id == id }

    @Test fun firstWorkoutUnlocksWithThatWorkout() {
        val w = workout("w1", monday, lift("bench_press", 50.0, 5))
        val result = Achievements.evaluate(listOf(w), emptyList(), 3, utc, monday)
        val first = status(result, "workouts_1")
        assertTrue(first.unlocked)
        assertEquals("w1", first.workoutId)
        assertFalse(status(result, "workouts_10").unlocked)
        assertEquals(1.0, status(result, "workouts_10").progress)
    }

    @Test fun strengthMilestoneUsesTheEstimate() {
        // 90 × 5 ≈ 105 kg estimated: past 100, not 140.
        val result = Achievements.evaluate(listOf(workout("w", monday, lift("bench_press", 90.0, 5))), emptyList(), 3, utc, monday)
        assertTrue(status(result, "bench_100").unlocked)
        assertFalse(status(result, "bench_140").unlocked)
    }

    @Test fun streakNeedsTheGoalEveryWeekWithoutGaps() {
        val weeks = listOf(0, 1, 3, 4, 5).flatMap { week ->
            (0 until 2).map { d -> workout("w$week-$d", LocalDate.fromEpochDays(monday.toEpochDays() + week * 7 + d * 2)) }
        }
        val result = Achievements.evaluate(weeks, emptyList(), 2, utc, LocalDate.fromEpochDays(monday.toEpochDays() + 35))
        assertTrue(status(result, "streak_2").unlocked)
        // Week 2 is empty: the streak restarts and reaches 3, not 5.
        assertFalse(status(result, "streak_4").unlocked)
        assertEquals("w1-1", status(result, "streak_2").workoutId)
    }

    @Test fun supersetEarlyBirdAndHyrox() {
        val w = workout("w", monday, lift("bench_press", 60.0, 8, group = 1), lift("barbell_row", 60.0, 8, group = 1), lift("wall_ball", 9.0, 20), hour = 6)
        val result = Achievements.evaluate(listOf(w), emptyList(), 3, utc, monday)
        assertTrue(status(result, "superset_1").unlocked)
        assertTrue(status(result, "early_bird_1").unlocked)
        assertTrue(status(result, "hyrox_1").unlocked)
    }

    @Test fun weighInsHaveNoWorkout() {
        val weights = (1..10).map { BodyWeight("b$it", it * HOUR, 80.0) }
        val weighIns = status(Achievements.evaluate(emptyList(), weights, 3, utc, monday), "weigh_ins_10")
        assertTrue(weighIns.unlocked)
        assertNull(weighIns.workoutId)
    }

    @Test fun unlockedByAWorkout() {
        val w = workout("w1", monday, lift("deadlift", 100.0, 1, sets = 60))
        val unlocked = Achievements.unlockedBy("w1", Achievements.evaluate(listOf(w), emptyList(), 3, utc, monday)).map { it.achievement.id }
        assertTrue("workouts_1" in unlocked)
        assertTrue("session_volume_5000" in unlocked)
        assertTrue("deadlift_100" in unlocked)
    }

    @Test fun idsAreUnique() = assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
}

class MonthRecapTest {
    @Test fun recapFindsFavouriteAndBiggestStep() {
        val workouts = listOf(
            workout("sep", LocalDate(2026, 9, 10), lift("bench_press", 80.0, 5)),
            workout("oct1", LocalDate(2026, 10, 5), lift("bench_press", 85.0, 5, sets = 3), lift("barbell_row", 60.0, 8)),
            workout("oct2", LocalDate(2026, 10, 20), lift("bench_press", 90.0, 5, sets = 3)),
        )
        val achievements = Achievements.evaluate(workouts, emptyList(), 3, utc, LocalDate(2026, 11, 1))
        val recap = MonthRecaps.build(2026, Month.OCTOBER, workouts, achievements, utc, ExerciseCatalog::find)
        assertEquals(2, recap.stats.workouts)
        assertEquals("bench_press", recap.favoriteExerciseId)
        assertEquals(6, recap.favoriteSets)
        assertNotNull(recap.topGain)
        assertEquals("bench_press", recap.topGain!!.exerciseId)
        assertTrue(recap.unlocked.none { it.achievement.id == "workouts_1" }) // that was September
    }
}
