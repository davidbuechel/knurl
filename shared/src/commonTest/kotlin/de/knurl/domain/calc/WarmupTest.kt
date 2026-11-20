package de.knurl.domain.calc

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.SetKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarmupTest {
    @Test fun heavyBarbellGetsTheFullLadder() {
        val sets = Warmup.sets(100.0, Equipment.BARBELL)
        assertEquals(listOf(20.0, 40.0, 60.0, 80.0), sets.map { it.weightKg })
        assertEquals(listOf(10, 8, 5, 3), sets.map { it.reps })
        assertTrue(sets.all { it.kind == SetKind.WARMUP && !it.done })
    }

    @Test fun lightBarbellSkipsStepsBelowTheBar() =
        assertEquals(listOf(25.0), Warmup.sets(40.0, Equipment.BARBELL).map { it.weightKg }.filter { it > 20.0 }.take(1))

    @Test fun dumbbellsUseTwoKiloSteps() =
        assertEquals(listOf(12.0, 18.0, 24.0), Warmup.sets(30.0, Equipment.DUMBBELL).map { it.weightKg })

    @Test fun noWeightNoWarmup() = assertTrue(Warmup.sets(0.0, Equipment.MACHINE).isEmpty())
}
