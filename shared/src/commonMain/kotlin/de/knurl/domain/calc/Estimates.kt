package de.knurl.domain.calc

import de.knurl.domain.model.Routine
import kotlin.math.roundToInt

/** Rough numbers shown before a workout starts. */
object Estimates {
    /** Seconds one set takes, without the rest after it. */
    private const val SET_SECONDS = 40

    /** Minutes for a routine, rounded to 5: every set plus its rest; supersets rest once per round. */
    fun minutes(routine: Routine): Int {
        if (routine.items.isEmpty()) return 0
        val seconds = routine.items.mapIndexed { i, item ->
            val nextInSameSuperset = item.group != null && routine.items.getOrNull(i + 1)?.group == item.group
            item.sets * (SET_SECONDS + if (nextInSameSuperset) 0 else item.restSec)
        }.sum()
        return ((seconds / 60.0) / 5).roundToInt().coerceAtLeast(1) * 5
    }

    fun sets(routine: Routine): Int = routine.items.sumOf { it.sets }
}
