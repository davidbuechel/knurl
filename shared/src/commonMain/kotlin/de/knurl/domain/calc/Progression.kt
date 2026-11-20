package de.knurl.domain.calc

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.RepRange
import de.knurl.domain.model.SetEntry

/**
 * Double progression: stay at a weight until every working set reaches the top of the rep range,
 * then add the smallest sensible jump for that equipment and start again at the bottom.
 */
object Progression {

    data class Suggestion(val weightKg: Double, val reps: Int, val increased: Boolean)

    fun increment(equipment: Equipment): Double = when (equipment) {
        Equipment.BARBELL, Equipment.EZ_BAR, Equipment.SMITH, Equipment.MACHINE, Equipment.CABLE -> 2.5
        Equipment.DUMBBELL, Equipment.KETTLEBELL -> 2.0
        Equipment.BODYWEIGHT, Equipment.BAND -> 0.0
    }

    /** Suggestion for the next session from the last session's sets, or null without a working set to go on. */
    fun suggest(lastSets: List<SetEntry>, range: RepRange, equipment: Equipment): Suggestion? {
        val working = lastSets.filter { it.kind.counts && it.reps > 0 }
        if (working.isEmpty()) return null
        val weight = working.maxOf { it.weightKg }
        val atWeight = working.filter { it.weightKg == weight }
        val step = increment(equipment)

        return if (atWeight.all { it.reps >= range.max }) {
            if (step > 0.0) Suggestion(weight + step, range.min, increased = true)
            else Suggestion(weight, atWeight.maxOf { it.reps } + 1, increased = true)
        } else {
            val target = (atWeight.minOf { it.reps } + 1).coerceIn(range.min, range.max)
            Suggestion(weight, target, increased = false)
        }
    }
}
