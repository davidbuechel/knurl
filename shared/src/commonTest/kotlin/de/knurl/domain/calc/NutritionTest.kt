package de.knurl.domain.calc

import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Food
import de.knurl.domain.model.FoodEntry
import de.knurl.domain.model.NutritionGoal
import de.knurl.domain.model.WeightGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NutritionTest {
    @Test fun targetsWithoutHeightUseBodyWeight() {
        // 80 kg x 22 = 1760, x 1.5 (3 days) = 2640, keep -> 2650; protein 80 x 1.6 = 128 -> 130
        assertEquals(Nutrition.Targets(2650, 130), Nutrition.targets(NutritionGoal(), 80.0, null, null, 3))
    }

    @Test fun targetsWithHeightAgeAndSexUseMifflin() {
        // 10x80 + 6.25x180 - 5x30 + 5 = 1780, x 1.5 = 2670, lose: -534 -> 2136 -> 2150; protein 160
        assertEquals(Nutrition.Targets(2150, 160), Nutrition.targets(NutritionGoal(WeightGoal.LOSE, heightCm = 180), 80.0, 30, true, 3))
    }

    @Test fun proteinStopsGrowingAt120Kilos() {
        assertEquals(240, Nutrition.targets(NutritionGoal(WeightGoal.LOSE), 180.0, null, null, 3).proteinG)
    }

    @Test fun handSetTargetsWin() {
        assertEquals(
            Nutrition.Targets(3000, 200),
            Nutrition.targets(NutritionGoal(WeightGoal.GAIN, kcalOverride = 3000, proteinOverride = 200), 80.0, null, null, 3),
        )
    }

    @Test fun portionsScaleFromPer100() {
        val skyr = Food("f", "Skyr", "Arla", kcalPer100 = 63.0, proteinPer100 = 11.0, carbsPer100 = 4.0, fatPer100 = 0.2)
        val entry = Nutrition.portion(skyr, 250.0, "e", "2026-11-23", 0)
        assertEquals(158, entry.kcal)
        assertEquals(27.5, entry.proteinG)
        assertEquals("Skyr (Arla)", entry.name)
    }

    @Test fun totalsAddUp() {
        val entries = listOf(FoodEntry("a", "d", 0, "A", 500, 30.0, 50.0, 10.0), FoodEntry("b", "d", 0, "B", 250, 20.5))
        assertEquals(Nutrition.Totals(750, 50.5, 50.0, 10.0), Nutrition.totals(entries))
    }

    @Test fun weeklyTrendNeedsAWeekOfData() {
        val day = Progress.DAY_MS
        val now = 30 * day
        val losing = (0..3).map { BodyWeight("w$it", now - (21 - it * 7) * day, 82.0 - it * 0.5) }
        assertEquals(-0.5, Nutrition.weeklyTrend(losing, now))
        assertNull(Nutrition.weeklyTrend(losing.take(2), now))
    }
}
