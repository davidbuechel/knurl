package de.knurl.domain.search

import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Muscle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseSearchTest {
    private val all = ExerciseCatalog.core

    private fun first(query: String, german: Boolean = true) = ExerciseSearch.search(all, query, german = german).firstOrNull()?.id

    @Test fun umlautsDoNotMatter() {
        assertEquals("bench_press", first("bankdrücken"))
        assertEquals("bench_press", first("bankdrucken"))
        assertEquals("bench_press", first("Bankdruecken"))
    }

    @Test fun prefixFindsIt() = assertEquals("deadlift", first("kreuzhe"))

    @Test fun gymShorthand() {
        assertTrue(ExerciseSearch.search(all, "RDL").any { it.id == "romanian_deadlift" })
        assertTrue(ExerciseSearch.search(all, "OHP").any { it.id == "overhead_press" })
    }

    @Test fun oneTypoIsForgiven() = assertEquals("lat_pulldown", first("latzgu"))

    @Test fun theOtherLanguageWorksToo() = assertEquals("bench_press", first("bench press"))

    @Test fun muscleNamesFindExercises() {
        val result = ExerciseSearch.search(all, "bizeps")
        assertTrue(result.isNotEmpty() && result.all { it.primary == Muscle.BICEPS })
    }

    @Test fun everyWordMustMatch() = assertTrue(ExerciseSearch.search(all, "kurzhantel kreuzheben").isEmpty())

    @Test fun filtersApply() {
        val result = ExerciseSearch.search(all, "", ExerciseFilter(equipment = Equipment.MACHINE))
        assertTrue(result.isNotEmpty() && result.all { it.equipment == Equipment.MACHINE })
    }

    @Test fun distanceCountsSwapsAsOne() = assertEquals(1, ExerciseSearch.distance("latzgu", "latzug"))
}
