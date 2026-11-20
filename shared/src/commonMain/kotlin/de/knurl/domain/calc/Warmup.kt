package de.knurl.domain.calc

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import kotlin.math.roundToInt

/**
 * Warm-up sets before the first working set: the empty bar, then about 40, 60 and 80 % with
 * fewer reps each time. Steps too close to the bar or to each other are dropped.
 */
object Warmup {

    fun sets(workingKg: Double, equipment: Equipment, barKg: Double = 20.0): List<SetEntry> {
        if (workingKg <= 0.0) return emptyList()
        val step = Progression.increment(equipment).takeIf { it > 0 } ?: 2.5
        val usesBar = equipment == Equipment.BARBELL || equipment == Equipment.SMITH || equipment == Equipment.EZ_BAR
        val plan = mutableListOf<SetEntry>()
        if (usesBar && workingKg > barKg * 1.5) plan += warmup(barKg, 10)
        for ((percent, reps) in listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)) {
            val kg = round(workingKg * percent, step)
            val previous = plan.lastOrNull()?.weightKg ?: 0.0
            if (kg > previous + step / 2 && kg < workingKg && (!usesBar || kg > barKg)) plan += warmup(kg, reps)
        }
        return plan
    }

    private fun warmup(kg: Double, reps: Int) = SetEntry(kind = SetKind.WARMUP, weightKg = kg, reps = reps)

    private fun round(kg: Double, step: Double) = (kg / step).roundToInt() * step
}
