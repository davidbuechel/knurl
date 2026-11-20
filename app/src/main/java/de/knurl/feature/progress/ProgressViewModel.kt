package de.knurl.feature.progress

import de.knurl.domain.calc.Calories
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Improvement
import de.knurl.domain.calc.LoadLevel
import de.knurl.domain.calc.MuscleLoad
import de.knurl.domain.calc.Period
import de.knurl.domain.calc.PeriodStats
import de.knurl.domain.calc.Periods
import de.knurl.domain.calc.Progress
import de.knurl.domain.calc.Weeks
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Muscle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class ImproverRow(val improvement: Improvement, val exercise: Exercise?)

data class ProgressState(
    val period: Period,
    val stats: PeriodStats,
    val previous: PeriodStats,
    /** Estimated energy in the period, null without any body weight. */
    val kcal: Int?,
    val kcalPrevious: Int?,
    /** Hard sets per muscle as a weekly average over the period so far, every muscle in body order. */
    val muscleWeekly: List<Pair<Muscle, Double>>,
    val levels: Map<Muscle, LoadLevel>,
    val improvers: List<ImproverRow>,
    val volumeBuckets: List<Double>,
    /** First day of each bucket, for the labels. */
    val bucketStarts: List<LocalDate>,
    val streakWeeks: Int,
    val weeklyGoal: Int,
    val bodyWeights: List<BodyWeight>,
    val trainingDays: Set<java.time.LocalDate>,
)

class ProgressViewModel(private val training: TrainingRepository) : ViewModel() {

    private val period = MutableStateFlow(Period.WEEK)

    fun setPeriod(value: Period) {
        period.value = value
    }

    fun addBodyWeight(kg: Double) = training.addBodyWeight(kg)

    fun deleteBodyWeight(id: String) = training.deleteBodyWeight(id)

    val state: StateFlow<ProgressState?> = combine(training.data.filterNotNull(), period) { data, p ->
        val zone = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Weeks.dateOf(nowMs, zone)
        val records = History.records(data.workouts)
        val range = Periods.range(p, today)
        val inside = Periods.inRange(data.workouts, range, zone)
        val weekly = MuscleLoad.weekly(inside, training::exercise, Periods.weeksElapsed(range, today))
        val buckets = Periods.volumeBuckets(data.workouts, p, today, zone)
        val bucketUnit = if (p == Period.WEEK) Period.WEEK else Period.MONTH
        ProgressState(
            period = p,
            stats = Periods.stats(data.workouts, range, zone, records),
            previous = Periods.stats(data.workouts, Periods.previousSoFar(p, today), zone, records),
            kcal = Calories.total(inside, data.bodyWeights),
            kcalPrevious = Calories.total(Periods.inRange(data.workouts, Periods.previousSoFar(p, today), zone), data.bodyWeights),
            muscleWeekly = Muscle.entries.map { it to (weekly[it] ?: 0.0) },
            levels = Muscle.entries.associateWith { MuscleLoad.level(weekly[it] ?: 0.0) },
            improvers = Progress.topImprovers(data.workouts, nowMs, improverDays(p)).map { ImproverRow(it, training.exercise(it.exerciseId)) },
            volumeBuckets = buckets,
            bucketStarts = (buckets.lastIndex downTo 0).map { Periods.range(bucketUnit, today, -it).start },
            streakWeeks = Weeks.streak(data.workouts, today, zone, data.settings.weeklyGoal),
            weeklyGoal = data.settings.weeklyGoal,
            bodyWeights = data.bodyWeights,
            trainingDays = data.workouts.map {
                java.time.Instant.ofEpochMilli(it.startedAtMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        /** How far back "biggest gains" looks: a week alone is too short to show progress. */
        fun improverDays(period: Period) = when (period) {
            Period.WEEK -> 28
            Period.MONTH -> 90
            Period.YEAR -> 365
        }
    }
}
