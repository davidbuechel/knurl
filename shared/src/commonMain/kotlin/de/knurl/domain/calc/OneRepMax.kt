package de.knurl.domain.calc

import de.knurl.domain.model.SetEntry

/** Estimated one-rep max (Epley). Sets above [MAX_REPS] are too far from a max to say much, so they don't count. */
object OneRepMax {
    const val MAX_REPS = 12

    fun epley(weightKg: Double, reps: Int): Double? = when {
        weightKg <= 0.0 || reps <= 0 || reps > MAX_REPS -> null
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    fun of(set: SetEntry): Double? = if (set.kind.counts) epley(set.weightKg, set.reps) else null

    fun best(sets: Iterable<SetEntry>): Double? = sets.mapNotNull(::of).maxOrNull()
}
