package de.knurl.core.food

import de.knurl.domain.model.Food
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenFoodFactsTest {
    private fun food(name: String, brand: String = "") = Food("off_$name$brand", name, brand, kcalPer100 = 60.0, proteinPer100 = 10.0)

    @Test fun productsNamedLikeTheSearchComeFirstAndOthersDrop() {
        val found = listOf(food("Fromage Blanc Nature"), food("Skyr nature 0%", "Yoplait"), food("Skyr", "Arla"))
        assertEquals(listOf("Skyr nature 0%", "Skyr"), OpenFoodFacts.relevant(found, "skyr").map { it.name })
    }

    @Test fun withoutAnyNameMatchEverythingStays() {
        val found = listOf(food("Fromage Blanc"), food("Quark"))
        assertEquals(2, OpenFoodFacts.relevant(found, "skyr").size)
    }

    @Test fun duplicatesOfTheSameProductCollapse() {
        val found = listOf(food("Skyr", "Arla"), food("skyr", "ARLA"))
        assertEquals(1, OpenFoodFacts.relevant(found, "skyr").size)
    }
}
