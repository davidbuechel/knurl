package de.knurl.domain.achievements

import de.knurl.domain.calc.History
import de.knurl.domain.calc.OneRepMax
import de.knurl.domain.calc.Volume
import de.knurl.domain.calc.Weeks
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Workout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class Tier { BRONZE, SILVER, GOLD }

enum class AchievementKind {
    WORKOUTS, STREAK, SESSION_VOLUME, TOTAL_VOLUME, BENCH, SQUAT, DEADLIFT, RECORDS, SUPERSET, EARLY_BIRD, HYROX, WEIGH_INS,
}

/** One achievement: reach [target] of its kind (workouts, weeks, kilograms, …). */
data class Achievement(val kind: AchievementKind, val target: Double, val tier: Tier) {
    val id: String get() = "${kind.name.lowercase()}_${target.toLong()}"
}

/** Where someone stands: progress towards the target, and when and with which workout it was unlocked. */
data class AchievementStatus(
    val achievement: Achievement,
    val progress: Double,
    val unlockedAtMs: Long?,
    /** The workout that unlocked it; null for body weight or when it came from several. */
    val workoutId: String?,
) {
    val unlocked: Boolean get() = unlockedAtMs != null
    val fraction: Float get() = (progress / achievement.target).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Achievements are derived, never stored, like records: one pass through the history in time order
 * finds when each was reached. Nothing can drift apart, and editing an old workout updates them.
 */
object Achievements {

    /** The core exercises the strength milestones are measured on. */
    const val BENCH_ID = "bench_press"
    const val SQUAT_ID = "back_squat"
    const val DEADLIFT_ID = "deadlift"
    val HYROX_IDS = setOf("sled_push", "sled_pull", "wall_ball", "ski_erg", "farmers_carry", "sandbag_lunge", "burpee")

    /** Workouts started before this hour count for "early bird". */
    const val EARLY_HOUR = 7

    val all: List<Achievement> = buildList {
        fun add(kind: AchievementKind, vararg steps: Pair<Number, Tier>) = steps.forEach { (t, tier) -> add(Achievement(kind, t.toDouble(), tier)) }
        add(AchievementKind.WORKOUTS, 1 to Tier.BRONZE, 10 to Tier.BRONZE, 50 to Tier.SILVER, 100 to Tier.GOLD, 250 to Tier.GOLD)
        add(AchievementKind.STREAK, 2 to Tier.BRONZE, 4 to Tier.BRONZE, 8 to Tier.SILVER, 12 to Tier.SILVER, 26 to Tier.GOLD)
        add(AchievementKind.SESSION_VOLUME, 5_000 to Tier.BRONZE, 10_000 to Tier.SILVER, 20_000 to Tier.GOLD)
        add(AchievementKind.TOTAL_VOLUME, 100_000 to Tier.SILVER, 1_000_000 to Tier.GOLD)
        add(AchievementKind.BENCH, 60 to Tier.BRONZE, 100 to Tier.SILVER, 140 to Tier.GOLD)
        add(AchievementKind.SQUAT, 100 to Tier.BRONZE, 140 to Tier.SILVER, 180 to Tier.GOLD)
        add(AchievementKind.DEADLIFT, 100 to Tier.BRONZE, 180 to Tier.SILVER, 220 to Tier.GOLD)
        add(AchievementKind.RECORDS, 10 to Tier.BRONZE, 50 to Tier.SILVER, 100 to Tier.GOLD)
        add(AchievementKind.SUPERSET, 1 to Tier.BRONZE)
        add(AchievementKind.EARLY_BIRD, 1 to Tier.BRONZE)
        add(AchievementKind.HYROX, 1 to Tier.BRONZE)
        add(AchievementKind.WEIGH_INS, 10 to Tier.BRONZE)
    }

    fun evaluate(workouts: List<Workout>, bodyWeights: List<BodyWeight>, weeklyGoal: Int, zone: TimeZone, today: LocalDate): List<AchievementStatus> {
        val finished = workouts.filter { it.isFinished }.sortedBy { it.startedAtMs }
        val current = mutableMapOf<AchievementKind, Double>()
        val unlocked = mutableMapOf<String, Pair<Long, String?>>()

        fun reach(kind: AchievementKind, value: Double, atMs: Long, workoutId: String?) {
            current[kind] = maxOf(current[kind] ?: 0.0, value)
            all.filter { it.kind == kind && value >= it.target && it.id !in unlocked }.forEach { unlocked[it.id] = atMs to workoutId }
        }

        val recordsByWorkout = History.records(finished).groupingBy { it.workoutId }.eachCount()
        var count = 0
        var total = 0.0
        var records = 0
        val best = mutableMapOf<String, Double>()
        for (w in finished) {
            count++
            val volume = Volume.of(w)
            total += volume
            records += recordsByWorkout[w.id] ?: 0
            reach(AchievementKind.WORKOUTS, count.toDouble(), w.startedAtMs, w.id)
            reach(AchievementKind.SESSION_VOLUME, volume, w.startedAtMs, w.id)
            reach(AchievementKind.TOTAL_VOLUME, total, w.startedAtMs, w.id)
            reach(AchievementKind.RECORDS, records.toDouble(), w.startedAtMs, w.id)
            for ((kind, id) in listOf(AchievementKind.BENCH to BENCH_ID, AchievementKind.SQUAT to SQUAT_ID, AchievementKind.DEADLIFT to DEADLIFT_ID)) {
                val e1rm = w.exercises.filter { it.exerciseId == id }.mapNotNull { OneRepMax.best(it.sets) }.maxOrNull() ?: continue
                best[id] = maxOf(best[id] ?: 0.0, e1rm)
                reach(kind, best.getValue(id), w.startedAtMs, w.id)
            }
            val worked = w.exercises.filter { e -> e.sets.any { it.kind.counts } }
            if (worked.groupBy { it.group }.any { (group, items) -> group != null && items.size > 1 }) reach(AchievementKind.SUPERSET, 1.0, w.startedAtMs, w.id)
            if (Instant.fromEpochMilliseconds(w.startedAtMs).toLocalDateTime(zone).hour < EARLY_HOUR) reach(AchievementKind.EARLY_BIRD, 1.0, w.startedAtMs, w.id)
            if (worked.any { it.exerciseId in HYROX_IDS }) reach(AchievementKind.HYROX, 1.0, w.startedAtMs, w.id)
        }
        bodyWeights.sortedBy { it.atMs }.forEachIndexed { i, entry -> reach(AchievementKind.WEIGH_INS, (i + 1).toDouble(), entry.atMs, null) }
        streaks(finished, weeklyGoal, zone).forEach { (length, workout) -> reach(AchievementKind.STREAK, length.toDouble(), workout.startedAtMs, workout.id) }
        // Progress towards a streak is the streak running now, not the best one ever.
        current[AchievementKind.STREAK] = Weeks.streak(finished, today, zone, weeklyGoal).toDouble()

        return all.map { a ->
            val done = unlocked[a.id]
            AchievementStatus(a, if (done != null) a.target else current[a.kind] ?: 0.0, done?.first, done?.second)
        }
    }

    /**
     * Every point a streak grew: (length, the workout that met that week's goal). Weeks without
     * enough workouts, including empty ones in between, reset it.
     */
    private fun streaks(finished: List<Workout>, goal: Int, zone: TimeZone): List<Pair<Int, Workout>> {
        if (goal <= 0 || finished.isEmpty()) return emptyList()
        val byWeek = finished.groupBy { Weeks.startOf(Weeks.dateOf(it.startedAtMs, zone)) }
        val result = mutableListOf<Pair<Int, Workout>>()
        var week = byWeek.keys.min()
        val last = byWeek.keys.max()
        var length = 0
        while (week <= last) {
            val inWeek = byWeek[week].orEmpty()
            if (inWeek.size >= goal) {
                length++
                result += length to inWeek[goal - 1]
            } else {
                length = 0
            }
            week = week.plus(7, DateTimeUnit.DAY)
        }
        return result
    }

    /** What a workout unlocked, for the summary. */
    fun unlockedBy(workoutId: String, statuses: List<AchievementStatus>): List<AchievementStatus> =
        statuses.filter { it.workoutId == workoutId }
}
