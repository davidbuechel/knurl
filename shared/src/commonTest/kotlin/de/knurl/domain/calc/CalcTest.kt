package de.knurl.domain.calc

import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OneRepMaxTest {
    @Test fun singleIsTheWeightItself() = assertEquals(100.0, OneRepMax.epley(100.0, 1))
    @Test fun fiveRepsAtHundred() = assertEquals(116.666, OneRepMax.epley(100.0, 5)!!, 0.001)
    @Test fun highRepSetsDoNotCount() = assertNull(OneRepMax.epley(60.0, 13))
    @Test fun warmupsDoNotCount() = assertNull(OneRepMax.of(SetEntry(SetKind.WARMUP, 60.0, 5)))
}

class VolumeTest {
    @Test fun skipsWarmups() {
        val sets = listOf(SetEntry(SetKind.WARMUP, 60.0, 10), SetEntry(weightKg = 100.0, reps = 5), SetEntry(weightKg = 100.0, reps = 4))
        assertEquals(900.0, Volume.of(sets))
    }

    @Test fun hardSetsSplitPrimaryAndSecondary() {
        val workout = finished("w1", 0, "bench_press", SetEntry(weightKg = 100.0, reps = 5), SetEntry(weightKg = 100.0, reps = 5, rir = 5))
        val sets = HardSets.perMuscle(listOf(workout), ExerciseCatalog::find)
        assertEquals(1.0, sets[Muscle.CHEST])
        assertEquals(0.5, sets[Muscle.TRICEPS])
    }
}

class ProgressionTest {
    private val range = RepRange(8, 12)

    @Test fun addsWeightWhenAllSetsHitTheTop() {
        val s = Progression.suggest(List(3) { SetEntry(weightKg = 60.0, reps = 12) }, range, Equipment.BARBELL)!!
        assertEquals(62.5, s.weightKg)
        assertEquals(8, s.reps)
        assertTrue(s.increased)
    }

    @Test fun addsARepOtherwise() {
        val s = Progression.suggest(listOf(SetEntry(weightKg = 60.0, reps = 12), SetEntry(weightKg = 60.0, reps = 9)), range, Equipment.BARBELL)!!
        assertEquals(60.0, s.weightKg)
        assertEquals(10, s.reps)
        assertFalse(s.increased)
    }

    @Test fun dumbbellsJumpTwoKilos() {
        val s = Progression.suggest(listOf(SetEntry(weightKg = 30.0, reps = 12)), range, Equipment.DUMBBELL)!!
        assertEquals(32.0, s.weightKg)
    }

    @Test fun nothingToGoOn() = assertNull(Progression.suggest(emptyList(), range, Equipment.BARBELL))
}

class PlateCalculatorTest {
    @Test fun hundredKilos() = assertEquals(listOf(25.0, 15.0), PlateCalculator.load(100.0).perSide)

    @Test fun smallPlatesAddUpExactly() {
        val load = PlateCalculator.load(62.5)
        assertEquals(listOf(20.0, 1.25), load.perSide)
        assertTrue(load.isExact)
    }

    @Test fun reportsWhatIsMissing() {
        val load = PlateCalculator.load(61.0)
        assertEquals(1.0, load.missingKg, 0.0001)
    }

    @Test fun emptyBar() = assertTrue(PlateCalculator.load(20.0).perSide.isEmpty())
}

class HistoryTest {
    @Test fun firstSessionIsBaselineThenRecords() {
        val workouts = listOf(
            finished("a", 1, "bench_press", SetEntry(weightKg = 95.0, reps = 5)),
            finished("b", 2, "bench_press", SetEntry(weightKg = 97.5, reps = 5)),
            finished("c", 3, "bench_press", SetEntry(weightKg = 97.5, reps = 4)),
        )
        val records = History.records(workouts)
        assertEquals(listOf("b"), records.map { it.workoutId })
    }

    @Test fun lastSetsComeFromTheNewestSession() {
        val workouts = listOf(
            finished("b", 2, "bench_press", SetEntry(weightKg = 100.0, reps = 5)),
            finished("a", 1, "bench_press", SetEntry(weightKg = 90.0, reps = 5)),
        )
        assertEquals(100.0, History.lastSets("bench_press", workouts).single().weightKg)
    }
}

class WeeksTest {
    private val zone = TimeZone.UTC

    @Test fun weekStartsOnMonday() =
        assertEquals(LocalDate(2026, 11, 16), Weeks.startOf(LocalDate(2026, 11, 20)))

    @Test fun streakIgnoresUnfinishedCurrentWeek() {
        // Two weeks before the current one met a goal of 1; the current week has nothing yet.
        val workouts = listOf(
            finished("a", ms(LocalDate(2026, 11, 3)), "bench_press", SetEntry(weightKg = 60.0, reps = 5)),
            finished("b", ms(LocalDate(2026, 11, 11)), "bench_press", SetEntry(weightKg = 60.0, reps = 5)),
        )
        assertEquals(2, Weeks.streak(workouts, LocalDate(2026, 11, 20), zone, goal = 1))
    }

    private fun ms(date: LocalDate): Long = date.toEpochDays() * 86_400_000L
}

private fun finished(id: String, atMs: Long, exerciseId: String, vararg sets: SetEntry) = Workout(
    id = id,
    name = id,
    startedAtMs = atMs,
    endedAtMs = atMs + 1,
    exercises = listOf(WorkoutExercise(exerciseId, sets.toList())),
)
