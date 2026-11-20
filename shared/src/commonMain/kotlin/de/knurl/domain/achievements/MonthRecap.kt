package de.knurl.domain.achievements

import de.knurl.domain.calc.ExerciseLookup
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Improvement
import de.knurl.domain.calc.LoadLevel
import de.knurl.domain.calc.MuscleLoad
import de.knurl.domain.calc.PeriodRange
import de.knurl.domain.calc.PeriodStats
import de.knurl.domain.calc.Periods
import de.knurl.domain.calc.Progress
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.Workout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/** A month told as a story: what was done, the favourite, the biggest step, what was unlocked. */
data class MonthRecap(
    val year: Int,
    val month: Month,
    val stats: PeriodStats,
    /** The exercise with the most counting sets this month. */
    val favoriteExerciseId: String?,
    val favoriteSets: Int,
    /** Best estimated 1RM this month against the best before it, the largest step in percent. */
    val topGain: Improvement?,
    val muscleLevels: Map<Muscle, LoadLevel>,
    val unlocked: List<AchievementStatus>,
)

object MonthRecaps {

    fun range(year: Int, month: Month): PeriodRange = LocalDate(year, month, 1).let { PeriodRange(it, it.plus(1, DateTimeUnit.MONTH)) }

    fun build(year: Int, month: Month, workouts: List<Workout>, achievements: List<AchievementStatus>, zone: TimeZone, lookup: ExerciseLookup): MonthRecap {
        val range = range(year, month)
        val inside = Periods.inRange(workouts, range, zone)
        val startMs = range.start.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMs = range.endExclusive.atStartOfDayIn(zone).toEpochMilliseconds()
        val favorite = inside.flatMap { w -> w.exercises }.groupBy { it.exerciseId }
            .mapValues { (_, items) -> items.sumOf { e -> e.sets.count { it.kind.counts } } }
            .maxByOrNull { it.value }
        val upToEnd = workouts.filter { it.startedAtMs < endMs }
        val topGain = inside.flatMap { w -> w.exercises.map { it.exerciseId } }.distinct().mapNotNull { id ->
            val series = Progress.oneRepMaxSeries(id, upToEnd)
            if (series.none { it.atMs < startMs }) return@mapNotNull null
            Progress.change(id, series, startMs)?.takeIf { it.deltaKg > 0 }
        }.maxByOrNull { it.percent }
        val weekly = MuscleLoad.weekly(inside, lookup, range.days / 7.0)
        return MonthRecap(
            year = year,
            month = month,
            stats = Periods.stats(workouts, range, zone, History.records(workouts)),
            favoriteExerciseId = favorite?.key,
            favoriteSets = favorite?.value ?: 0,
            topGain = topGain,
            muscleLevels = Muscle.entries.associateWith { MuscleLoad.level(weekly[it] ?: 0.0) },
            unlocked = achievements.filter { it.unlockedAtMs != null && it.unlockedAtMs in startMs until endMs },
        )
    }
}
