package de.knurl.domain.catalog

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.MovementPattern
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import de.knurl.domain.search.ExerciseFilter
import de.knurl.domain.search.ExerciseSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomExercisesTest {
    private val landmine = CustomExercises.build(
        "custom_1", "  Landmine Press  ", Muscle.SHOULDERS, listOf(Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.CHEST, Muscle.CORE, Muscle.TRICEPS),
        Equipment.BARBELL, note = "Elbow tucked",
    )

    @Test fun oneNameForBothLanguagesTrimmed() {
        assertEquals("Landmine Press", landmine.nameDe)
        assertEquals("Landmine Press", landmine.nameEn)
        assertTrue(landmine.isCustom)
    }

    @Test fun secondaryMusclesAreCleanedAndCapped() =
        assertEquals(listOf(Muscle.TRICEPS, Muscle.CHEST, Muscle.CORE), landmine.secondary)

    @Test fun patternFollowsTheMuscle() {
        assertEquals(MovementPattern.VERTICAL_PUSH, landmine.pattern)
        assertEquals(MovementPattern.ISOLATION, CustomExercises.defaultPattern(Muscle.BICEPS))
    }

    @Test fun usageCoversHistoryPlansAndTheRunningWorkout() {
        val used = Workout("w", "W", null, 0, 1, listOf(WorkoutExercise("custom_1", emptyList())))
        val plan = Routine("r", "R", listOf(RoutineItem("custom_1", sets = 3)), 0)
        assertTrue(CustomExercises.isUsed("custom_1", listOf(used), emptyList(), null))
        assertTrue(CustomExercises.isUsed("custom_1", emptyList(), listOf(plan), null))
        assertTrue(CustomExercises.isUsed("custom_1", emptyList(), emptyList(), used))
        assertFalse(CustomExercises.isUsed("custom_1", emptyList(), emptyList(), null))
    }

    @Test fun searchFindsOwnExercisesButNotArchivedOnes() {
        val all = ExerciseCatalog.core + landmine + landmine.copy(id = "custom_2", nameDe = "Landmine Row", nameEn = "Landmine Row", archived = true)
        assertEquals(listOf("custom_1"), ExerciseSearch.search(all, "landmine").map { it.id })
        assertEquals(listOf("custom_1"), ExerciseSearch.search(all, "", ExerciseFilter(customOnly = true)).map { it.id })
    }
}
