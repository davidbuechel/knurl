package de.knurl.domain.catalog

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.MovementPattern
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals

class RetiredExercisesTest {
    private fun ex(id: String, archived: Boolean = false, custom: Boolean = false) =
        Exercise(id, id, id, Muscle.CHEST, emptyList(), Equipment.CABLE, MovementPattern.ISOLATION, isCustom = custom, archived = archived)

    @Test fun retiredExercisesStayOfferedOnlyToThoseWhoUseThem() {
        val all = listOf(ex("bench_press"), ex("old_cable_press", archived = true), ex("old_fly", archived = true), ex("custom_x", archived = true, custom = true))
        val workout = Workout(
            id = "w", name = "w", startedAtMs = 0,
            exercises = listOf("old_cable_press", "custom_x").map { WorkoutExercise(it, listOf(SetEntry(weightKg = 10.0, reps = 10, done = true))) },
        )
        val used = RetiredExercises.usedIds(listOf(workout), emptyList(), null)
        val offered = RetiredExercises.offered(all, used).filterNot { it.archived }.map { it.id }
        // The used retired one comes back; the unused one and the deleted own exercise stay hidden.
        assertEquals(listOf("bench_press", "old_cable_press"), offered)
    }
}
