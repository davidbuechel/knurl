package de.knurl.domain.catalog

import de.knurl.domain.model.RepRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarterPlanTest {
    @Test fun daysPickTheSplit() {
        assertEquals(listOf("Full body A", "Full body B"), StarterPlan.routines(Goal.MUSCLE, Experience.BEGINNER, 3, german = false).map { it.name })
        assertEquals(listOf("Oberkörper", "Unterkörper"), StarterPlan.routines(Goal.MUSCLE, Experience.BEGINNER, 4, german = true).map { it.name })
        assertEquals(listOf("Push", "Pull", "Legs"), StarterPlan.routines(Goal.MUSCLE, Experience.BEGINNER, 6, german = false).map { it.name })
    }

    @Test fun goalSetsRepsAndExperienceSets() {
        val strength = StarterPlan.routines(Goal.STRENGTH, Experience.ADVANCED, 3, false).first().items.first()
        assertEquals(RepRange(3, 6), strength.repRange)
        assertEquals(4, strength.sets)
        val fitness = StarterPlan.routines(Goal.FITNESS, Experience.BEGINNER, 3, false).first().items.last()
        assertEquals(RepRange(12, 15), fitness.repRange)
        assertEquals(2, fitness.sets)
    }

    @Test fun everyExerciseExists() {
        for (days in 1..7) for (goal in Goal.entries) {
            val ids = StarterPlan.routines(goal, Experience.INTERMEDIATE, days, false).flatMap { r -> r.items.map { it.exerciseId } }
            assertTrue(ids.all { ExerciseCatalog.find(it) != null }, "unknown exercise in $days days $goal")
        }
    }
}
