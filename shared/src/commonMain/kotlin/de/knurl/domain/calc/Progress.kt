package de.knurl.domain.calc

import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout

/** One value at one time, e.g. the best estimated 1RM of a session. */
data class SeriesPoint(val atMs: Long, val value: Double)

/** How far an exercise came: best estimated 1RM before a point in time against the best since. */
data class Improvement(val exerciseId: String, val fromKg: Double, val toKg: Double, val series: List<SeriesPoint>) {
    val deltaKg: Double get() = toKg - fromKg
    val percent: Double get() = if (fromKg > 0) deltaKg / fromKg * 100 else 0.0
}

/** The top set of an exercise today against last time: heavier, or the same weight for more reps. */
data class SetDelta(val weightKg: Double, val reps: Int) {
    val improved: Boolean get() = weightKg > 0.0 || (weightKg == 0.0 && reps > 0)
}

/** Progress over time, derived from finished workouts like the records in [History]. */
object Progress {

    /** Best estimated 1RM per session of an exercise, oldest first. */
    fun oneRepMaxSeries(exerciseId: String, workouts: Iterable<Workout>): List<SeriesPoint> =
        History.sessions(exerciseId, workouts).mapNotNull { s -> s.bestOneRepMax?.let { SeriesPoint(s.atMs, it) } }

    /** The series from [sinceMs] on (all of it for null). */
    fun window(series: List<SeriesPoint>, sinceMs: Long?): List<SeriesPoint> =
        if (sinceMs == null) series else series.filter { it.atMs >= sinceMs }

    /**
     * Best value since [sinceMs] against the best value before it. Without anything before, the first
     * value in the window is the baseline. Null when there is nothing to compare.
     */
    fun change(exerciseId: String, series: List<SeriesPoint>, sinceMs: Long?): Improvement? {
        val inWindow = window(series, sinceMs)
        if (inWindow.isEmpty()) return null
        val before = series.filter { sinceMs != null && it.atMs < sinceMs }.maxOfOrNull { it.value }
        val from = before ?: inWindow.first().value
        val compared = if (before == null) inWindow.drop(1) else inWindow
        val to = compared.maxOfOrNull { it.value } ?: return null
        return Improvement(exerciseId, from, to, inWindow)
    }

    /**
     * Exercises that grew most in the last [days] days, by percent: best estimated 1RM in that time
     * against the best before it. Only exercises with history before the window qualify.
     */
    fun topImprovers(workouts: List<Workout>, nowMs: Long, days: Int = 28, limit: Int = 3): List<Improvement> {
        val since = nowMs - days * DAY_MS
        val ids = workouts.filter { it.isFinished && it.startedAtMs >= since }.flatMap { w -> w.exercises.map { it.exerciseId } }.distinct()
        return ids.mapNotNull { id ->
            val series = oneRepMaxSeries(id, workouts)
            if (series.none { it.atMs < since }) return@mapNotNull null
            change(id, series, since)?.takeIf { it.deltaKg > 0.0 }
        }.sortedByDescending { it.percent }.take(limit)
    }

    /**
     * Rep maxes: for 1 to [OneRepMax.MAX_REPS] reps, the heaviest counting set done for at least that
     * many reps. A 100 kg × 5 also answers "how much for 3?".
     */
    fun repMaxes(exerciseId: String, workouts: Iterable<Workout>): Map<Int, SetEntry> {
        val sets = History.sessions(exerciseId, workouts).flatMap { it.sets }.filter { it.kind.counts && it.reps > 0 && it.weightKg > 0 }
        return (1..OneRepMax.MAX_REPS).mapNotNull { reps ->
            sets.filter { it.reps >= reps }.maxByOrNull { it.weightKg }?.let { reps to it }
        }.toMap()
    }

    /** Top set (heaviest counting set, then most reps) of [now] against [before]; null if either is missing. */
    fun sinceLast(now: List<SetEntry>, before: List<SetEntry>): SetDelta? {
        val a = topSet(now) ?: return null
        val b = topSet(before) ?: return null
        val weight = a.weightKg - b.weightKg
        return SetDelta(weight, if (weight == 0.0) a.reps - b.reps else 0)
    }

    /** Heaviest counting set, then most reps: the set people compare. */
    fun topSet(sets: List<SetEntry>): SetEntry? =
        sets.filter { it.kind.counts && it.reps > 0 }.maxWithOrNull(compareBy<SetEntry> { it.weightKg }.thenBy { it.reps })

    const val DAY_MS = 24L * 60 * 60 * 1000
}
