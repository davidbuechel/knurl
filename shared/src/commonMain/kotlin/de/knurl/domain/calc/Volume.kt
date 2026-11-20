package de.knurl.domain.calc

import de.knurl.domain.model.Muscle
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout

/** Tonnage: sum of weight × reps over every counting set. Warm-ups are left out. */
object Volume {
    fun of(sets: Iterable<SetEntry>): Double = sets.filter { it.kind.counts }.sumOf { it.weightKg * it.reps }

    fun of(workout: Workout): Double = workout.exercises.sumOf { of(it.sets) }
}

/**
 * Hard sets per muscle: a counting set with at most [MAX_RIR] reps in reserve (or no RIR logged)
 * counts 1 for the primary muscle and 0.5 for each secondary muscle.
 */
object HardSets {
    const val MAX_RIR = 3

    /** Weekly target range most evidence points to for growth. */
    val weeklyTarget: ClosedFloatingPointRange<Double> = 10.0..20.0

    fun isHard(set: SetEntry): Boolean = set.kind.counts && (set.rir == null || set.rir <= MAX_RIR)

    fun perMuscle(workouts: Iterable<Workout>, lookup: ExerciseLookup): Map<Muscle, Double> {
        val totals = mutableMapOf<Muscle, Double>()
        for (workout in workouts) for (item in workout.exercises) {
            val exercise = lookup(item.exerciseId) ?: continue
            val hard = item.sets.count(::isHard)
            if (hard == 0) continue
            totals.add(exercise.primary, hard.toDouble())
            exercise.secondary.forEach { totals.add(it, hard * 0.5) }
        }
        return totals
    }

    private fun MutableMap<Muscle, Double>.add(muscle: Muscle, amount: Double) {
        this[muscle] = (this[muscle] ?: 0.0) + amount
    }
}
