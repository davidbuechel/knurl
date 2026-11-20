package de.knurl.domain.calc

import de.knurl.domain.model.Workout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Training weeks run Monday to Sunday (ISO). */
object Weeks {

    fun dateOf(epochMs: Long, zone: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date

    fun startOf(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun inWeek(workouts: Iterable<Workout>, weekStart: LocalDate, zone: TimeZone): List<Workout> {
        val end = weekStart.plus(7, DateTimeUnit.DAY)
        return workouts.filter { it.isFinished && dateOf(it.startedAtMs, zone).let { d -> d >= weekStart && d < end } }
    }

    /**
     * Consecutive weeks, ending now, with at least [goal] workouts. The current week only counts once
     * the goal is met, so an unfinished week never breaks the streak.
     */
    fun streak(workouts: Iterable<Workout>, today: LocalDate, zone: TimeZone, goal: Int): Int {
        if (goal <= 0) return 0
        val perWeek = workouts.filter { it.isFinished }
            .groupingBy { startOf(dateOf(it.startedAtMs, zone)) }
            .eachCount()
        var week = startOf(today)
        var count = 0
        if ((perWeek[week] ?: 0) >= goal) count++
        week = week.minus(7, DateTimeUnit.DAY)
        while ((perWeek[week] ?: 0) >= goal) {
            count++
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return count
    }
}
