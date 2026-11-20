package de.knurl.domain.calc

import de.knurl.domain.model.Muscle
import de.knurl.domain.model.Workout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

enum class Period { WEEK, MONTH, YEAR }

/** A calendar period: Monday to Sunday, a month or a year. */
data class PeriodRange(val start: LocalDate, val endExclusive: LocalDate) {
    val days: Int get() = start.daysUntil(endExclusive)
    operator fun contains(date: LocalDate) = date >= start && date < endExclusive
}

data class PeriodStats(
    val workouts: Int,
    val volumeKg: Double,
    val sets: Int,
    val trainingDays: Int,
    val records: Int,
    val minutes: Int,
)

/** A value against the same value one period earlier. */
data class Change(val now: Double, val before: Double) {
    /** Whole percent, null when there is no earlier value to compare with. */
    val percent: Int? get() = if (before > 0) ((now - before) / before * 100).toInt() else null
}

/** Weeks, months and years, each against the one before. */
object Periods {

    /** The period containing [today], or [offset] periods before it (-1 = the previous one). */
    fun range(period: Period, today: LocalDate, offset: Int = 0): PeriodRange = when (period) {
        Period.WEEK -> Weeks.startOf(today).plus(offset * 7, DateTimeUnit.DAY).let { PeriodRange(it, it.plus(7, DateTimeUnit.DAY)) }
        Period.MONTH -> LocalDate(today.year, today.month, 1).plus(offset, DateTimeUnit.MONTH).let { PeriodRange(it, it.plus(1, DateTimeUnit.MONTH)) }
        Period.YEAR -> LocalDate(today.year + offset, 1, 1).let { PeriodRange(it, it.plus(1, DateTimeUnit.YEAR)) }
    }

    /**
     * The previous period, but only as far as the current one has run: Monday to Wednesday against
     * last Monday to Wednesday, so a week in progress is not judged against a whole one.
     */
    fun previousSoFar(period: Period, today: LocalDate): PeriodRange {
        val current = range(period, today)
        val previous = range(period, today, -1)
        val end = previous.start.plus(current.start.daysUntil(today) + 1, DateTimeUnit.DAY)
        return PeriodRange(previous.start, if (end < previous.endExclusive) end else previous.endExclusive)
    }

    fun inRange(workouts: Iterable<Workout>, range: PeriodRange, zone: TimeZone): List<Workout> =
        workouts.filter { it.isFinished && Weeks.dateOf(it.startedAtMs, zone) in range }

    fun stats(workouts: List<Workout>, range: PeriodRange, zone: TimeZone, records: List<PersonalRecord>): PeriodStats {
        val inside = inRange(workouts, range, zone)
        val ids = inside.map { it.id }.toSet()
        return PeriodStats(
            workouts = inside.size,
            volumeKg = inside.sumOf(Volume::of),
            sets = inside.sumOf { w -> w.exercises.sumOf { e -> e.sets.count { it.kind.counts } } },
            trainingDays = inside.map { Weeks.dateOf(it.startedAtMs, zone) }.toSet().size,
            records = records.count { it.workoutId in ids },
            minutes = inside.sumOf { w -> (((w.endedAtMs ?: w.startedAtMs) - w.startedAtMs) / 60_000).toInt() },
        )
    }

    /**
     * Weeks the period has run so far (at least one), so a half-finished month is not judged
     * against a full month's target.
     */
    fun weeksElapsed(range: PeriodRange, today: LocalDate): Double {
        val end = if (today < range.endExclusive) today.plus(1, DateTimeUnit.DAY) else range.endExclusive
        return (range.start.daysUntil(end) / 7.0).coerceAtLeast(1.0)
    }

    /** Volume per bucket for a bar chart: 8 weeks, 6 months or 12 months, oldest first, current last. */
    fun volumeBuckets(workouts: List<Workout>, period: Period, today: LocalDate, zone: TimeZone): List<Double> {
        val (count, unit) = when (period) {
            Period.WEEK -> 8 to Period.WEEK
            Period.MONTH -> 6 to Period.MONTH
            Period.YEAR -> 12 to Period.MONTH
        }
        return ((count - 1) downTo 0).map { back -> inRange(workouts, range(unit, today, -back), zone).sumOf(Volume::of) }
    }
}

enum class LoadLevel { NONE, LOW, TARGET, HIGH }

/** Hard sets per muscle as a weekly average, judged against [HardSets.weeklyTarget]. */
object MuscleLoad {
    fun weekly(workouts: Iterable<Workout>, lookup: ExerciseLookup, weeks: Double): Map<Muscle, Double> =
        HardSets.perMuscle(workouts, lookup).mapValues { it.value / weeks.coerceAtLeast(1.0) }

    fun level(weeklySets: Double): LoadLevel = when {
        weeklySets <= 0.0 -> LoadLevel.NONE
        weeklySets < HardSets.weeklyTarget.start -> LoadLevel.LOW
        weeklySets <= HardSets.weeklyTarget.endInclusive -> LoadLevel.TARGET
        else -> LoadLevel.HIGH
    }
}
