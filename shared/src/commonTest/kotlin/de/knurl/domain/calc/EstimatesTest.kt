package de.knurl.domain.calc

import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import kotlin.test.Test
import kotlin.test.assertEquals

class EstimatesTest {
    @Test fun setsPlusRestRoundedToFive() {
        // 3 × (40 + 120) + 3 × (40 + 60) = 780 s = 13 min → 15
        val r = Routine("r", "r", listOf(RoutineItem("a", 3, restSec = 120), RoutineItem("b", 3, restSec = 60)))
        assertEquals(15, Estimates.minutes(r))
    }

    @Test fun supersetsRestOncePerRound() {
        // a rests 0 (b follows in the superset): 3 × 40 + 3 × (40 + 90) = 510 s = 8.5 min → 10
        val r = Routine("r", "r", listOf(RoutineItem("a", 3, restSec = 90, group = 1), RoutineItem("b", 3, restSec = 90, group = 1)))
        assertEquals(10, Estimates.minutes(r))
    }
}
