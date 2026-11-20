package de.knurl.domain.calc

import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Workout
import kotlin.math.roundToInt

/**
 * Energy estimate for a workout. Without a heart rate this is MET × body weight × time, the way the
 * Compendium of Physical Activities does it: an honest estimate, roughly ±25 %. The app says so.
 *
 * - Strength training ranges from [MET_LIGHT] (few sets an hour, long rests) to [MET_DENSE] (many
 *   sets, short rests), by sets per minute.
 * - Conditioning (HIIT/Hyrox stations) counts [MET_CONDITIONING].
 * - The mix follows the share of sets.
 */
object Calories {
    const val MET_LIGHT = 3.5
    const val MET_DENSE = 6.0
    const val MET_CONDITIONING = 8.0

    /** Sets per minute that mean "light" and "dense". */
    private const val SPM_LIGHT = 0.10
    private const val SPM_DENSE = 0.40

    /**
     * A workout left running is not burning energy: time is capped at ten minutes per set (heavy
     * singles with long rests stay below), at least 20. Matches [SPM_LIGHT].
     */
    private const val MAX_MINUTES_PER_SET = 10.0
    private const val MIN_MINUTES = 20.0

    /** A body weight entry further than this from the workout is too stale to use. */
    private const val WEIGHT_STALE_MS = 180L * 24 * 60 * 60 * 1000

    val CONDITIONING_IDS = setOf("burpee", "wall_ball", "sled_push", "sled_pull", "farmers_carry", "sandbag_lunge", "ski_erg", "mountain_climber")

    /** Minutes that count: the workout's length, capped by what its sets can plausibly take. */
    fun activeMinutes(workout: Workout): Double {
        val sets = countingSets(workout)
        val length = ((workout.endedAtMs ?: return 0.0) - workout.startedAtMs) / 60_000.0
        return length.coerceAtMost(maxOf(sets * MAX_MINUTES_PER_SET, MIN_MINUTES)).coerceAtLeast(0.0)
    }

    fun met(workout: Workout): Double {
        val minutes = activeMinutes(workout)
        val sets = countingSets(workout)
        if (minutes <= 0.0 || sets == 0) return MET_LIGHT
        val density = ((sets / minutes - SPM_LIGHT) / (SPM_DENSE - SPM_LIGHT)).coerceIn(0.0, 1.0)
        val strength = MET_LIGHT + (MET_DENSE - MET_LIGHT) * density
        val conditioning = workout.exercises.filter { it.exerciseId in CONDITIONING_IDS }.sumOf { e -> e.sets.count { it.kind.counts } }
        val share = conditioning.toDouble() / sets
        return strength * (1 - share) + MET_CONDITIONING * share
    }

    /** kcal for a workout, or null without a usable body weight. */
    fun estimate(workout: Workout, bodyWeights: List<BodyWeight>): Int? {
        val weight = bodyWeightAt(workout.startedAtMs, bodyWeights) ?: return null
        val hours = activeMinutes(workout) / 60.0
        if (hours <= 0.0) return null
        return (met(workout) * weight * hours).roundToInt()
    }

    /** Sum over workouts; null when not one of them has a usable body weight. */
    fun total(workouts: List<Workout>, bodyWeights: List<BodyWeight>): Int? {
        val each = workouts.mapNotNull { estimate(it, bodyWeights) }
        return if (each.isEmpty()) null else each.sum()
    }

    /** The latest weight up to the workout; otherwise the first one after it. Stale entries don't count. */
    fun bodyWeightAt(atMs: Long, bodyWeights: List<BodyWeight>): Double? {
        val before = bodyWeights.filter { it.atMs <= atMs }.maxByOrNull { it.atMs }
        if (before != null && atMs - before.atMs <= WEIGHT_STALE_MS) return before.weightKg
        val after = bodyWeights.filter { it.atMs > atMs }.minByOrNull { it.atMs }
        return after?.takeIf { it.atMs - atMs <= WEIGHT_STALE_MS }?.weightKg
    }

    /**
     * With an average heart rate (from a watch via Health Connect): Keytel et al. 2005. Age and sex
     * make it much better; without them the MET estimate stays the honest choice, so this returns null.
     */
    fun fromHeartRate(averageBpm: Double, minutes: Double, weightKg: Double, age: Int?, male: Boolean?): Int? {
        if (age == null || male == null || minutes <= 0 || averageBpm <= 0) return null
        val perMinute = if (male) (-55.0969 + 0.6309 * averageBpm + 0.1988 * weightKg + 0.2017 * age) / 4.184
        else (-20.4022 + 0.4472 * averageBpm - 0.1263 * weightKg + 0.074 * age) / 4.184
        return (perMinute * minutes).roundToInt().takeIf { it > 0 }
    }

    private fun countingSets(workout: Workout) = workout.exercises.sumOf { e -> e.sets.count { it.kind.counts } }
}
