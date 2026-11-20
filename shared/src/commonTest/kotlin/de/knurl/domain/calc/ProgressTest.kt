package de.knurl.domain.calc

import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DAY = Progress.DAY_MS
private val utc = TimeZone.UTC

private fun session(id: String, day: Int, exerciseId: String, vararg sets: SetEntry) =
    Workout(id, id, null, day * DAY, day * DAY + 3_600_000, listOf(WorkoutExercise(exerciseId, sets.toList())))

private fun set(kg: Double, reps: Int) = SetEntry(weightKg = kg, reps = reps)

class ProgressTest {
    private val bench = listOf(
        session("a", 0, "bench_press", set(80.0, 5)),
        session("b", 20, "bench_press", set(85.0, 5)),
        session("c", 40, "bench_press", set(90.0, 5), set(95.0, 1)),
    )

    @Test fun seriesHasTheBestEstimatePerSession() {
        val series = Progress.oneRepMaxSeries("bench_press", bench)
        assertEquals(3, series.size)
        assertEquals(105.0, series.last().value, 0.001) // 90 × 5 beats 95 × 1
    }

    @Test fun changeComparesTheWindowWithTheBestBefore() {
        val series = Progress.oneRepMaxSeries("bench_press", bench)
        val change = Progress.change("bench_press", series, sinceMs = 10 * DAY)!!
        assertEquals(80.0 * (1 + 5 / 30.0), change.fromKg, 0.001)
        assertEquals(105.0, change.toKg, 0.001)
        assertEquals(2, change.series.size)
    }

    @Test fun changeOverEverythingStartsAtTheFirstSession() {
        val change = Progress.change("bench_press", Progress.oneRepMaxSeries("bench_press", bench), sinceMs = null)!!
        assertEquals(80.0 * (1 + 5 / 30.0), change.fromKg, 0.001)
    }

    @Test fun topImproversNeedHistoryBeforeTheWindow() {
        val newcomer = session("n", 40, "deadlift", set(140.0, 5))
        val top = Progress.topImprovers(bench + newcomer, nowMs = 41 * DAY, days = 28)
        assertEquals(listOf("bench_press"), top.map { it.exerciseId })
        assertTrue(top.single().percent > 0)
    }

    @Test fun repMaxesTakeHeavierSetsWithMoreReps() {
        val maxes = Progress.repMaxes("bench_press", bench)
        assertEquals(95.0, maxes.getValue(1).weightKg)
        assertEquals(90.0, maxes.getValue(3).weightKg) // 90 × 5 also answers 3
        assertNull(maxes[6])
    }

    @Test fun sinceLastPrefersWeightThenReps() {
        assertEquals(SetDelta(2.5, 0), Progress.sinceLast(listOf(set(82.5, 5)), listOf(set(80.0, 8))))
        assertEquals(SetDelta(0.0, 2), Progress.sinceLast(listOf(set(80.0, 7)), listOf(set(80.0, 5))))
        assertNull(Progress.sinceLast(listOf(SetEntry(SetKind.WARMUP, 40.0, 10)), listOf(set(80.0, 5))))
    }
}

class PeriodsTest {
    private val today = LocalDate(2026, 11, 18) // a Wednesday

    @Test fun rangesFollowTheCalendar() {
        assertEquals(PeriodRange(LocalDate(2026, 11, 16), LocalDate(2026, 11, 23)), Periods.range(Period.WEEK, today))
        assertEquals(PeriodRange(LocalDate(2026, 10, 1), LocalDate(2026, 11, 1)), Periods.range(Period.MONTH, today, -1))
        assertEquals(PeriodRange(LocalDate(2025, 1, 1), LocalDate(2026, 1, 1)), Periods.range(Period.YEAR, today, -1))
    }

    @Test fun previousPeriodOnlyAsFarAsTheCurrentOne() {
        assertEquals(PeriodRange(LocalDate(2026, 11, 9), LocalDate(2026, 11, 12)), Periods.previousSoFar(Period.WEEK, today))
        assertEquals(PeriodRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 19)), Periods.previousSoFar(Period.MONTH, today))
        // March 31st against February: capped at the end of February.
        assertEquals(LocalDate(2026, 3, 1), Periods.previousSoFar(Period.MONTH, LocalDate(2026, 3, 31)).endExclusive)
    }

    @Test fun statsCountWithinTheRange() {
        val nov = LocalDate(2026, 11, 2).toEpochDays().toInt()
        val workouts = listOf(
            session("in1", nov, "bench_press", set(100.0, 5)),
            session("in2", nov, "bench_press", set(105.0, 5)),
            session("out", nov - 10, "bench_press", set(100.0, 5)),
        )
        val stats = Periods.stats(workouts, Periods.range(Period.MONTH, today), utc, History.records(workouts))
        assertEquals(2, stats.workouts)
        assertEquals(1, stats.trainingDays)
        assertEquals(1025.0, stats.volumeKg)
        assertEquals(1, stats.records)
        assertEquals(120, stats.minutes)
    }

    @Test fun changeInPercent() {
        assertEquals(50, Change(150.0, 100.0).percent)
        assertNull(Change(10.0, 0.0).percent)
    }

    @Test fun aRunningMonthCountsTheWeeksSoFar() {
        assertEquals(18 / 7.0, Periods.weeksElapsed(Periods.range(Period.MONTH, today), today), 0.001)
        assertEquals(1.0, Periods.weeksElapsed(Periods.range(Period.WEEK, today), today))
    }

    @Test fun muscleLoadLevels() {
        assertEquals(LoadLevel.NONE, MuscleLoad.level(0.0))
        assertEquals(LoadLevel.LOW, MuscleLoad.level(6.0))
        assertEquals(LoadLevel.TARGET, MuscleLoad.level(12.0))
        assertEquals(LoadLevel.HIGH, MuscleLoad.level(24.0))
        val weekly = MuscleLoad.weekly(listOf(session("w", 0, "bench_press", set(100.0, 5), set(100.0, 5))), ExerciseCatalog::find, weeks = 2.0)
        assertEquals(1.0, weekly[Muscle.CHEST])
    }
}
