package de.knurl.domain.catalog

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.MovementPattern
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout

/** Rules for the person's own exercises. */
object CustomExercises {
    const val NAME_MAX = 80
    const val NOTE_MAX = 500
    const val SECONDARY_MAX = 3

    /**
     * A new or edited own exercise. One name is enough: it is used in both languages. The movement
     * pattern follows from the muscle when not given, so statistics and warm-ups have something to go on.
     */
    fun build(
        id: String,
        name: String,
        primary: Muscle,
        secondary: List<Muscle>,
        equipment: Equipment,
        note: String,
        pattern: MovementPattern = defaultPattern(primary),
    ): Exercise {
        val clean = name.trim().take(NAME_MAX)
        return Exercise(
            id = id,
            nameDe = clean,
            nameEn = clean,
            primary = primary,
            secondary = secondary.filter { it != primary }.distinct().take(SECONDARY_MAX),
            equipment = equipment,
            pattern = pattern,
            isCustom = true,
            note = note.trim().take(NOTE_MAX),
        )
    }

    /** A sensible movement pattern for a muscle; isolation for the small ones. */
    fun defaultPattern(primary: Muscle): MovementPattern = when (primary) {
        Muscle.CHEST -> MovementPattern.HORIZONTAL_PUSH
        Muscle.BACK -> MovementPattern.HORIZONTAL_PULL
        Muscle.SHOULDERS -> MovementPattern.VERTICAL_PUSH
        Muscle.QUADS -> MovementPattern.SQUAT
        Muscle.HAMSTRINGS, Muscle.GLUTES -> MovementPattern.HINGE
        Muscle.CORE -> MovementPattern.CORE
        else -> MovementPattern.ISOLATION
    }

    /** Used anywhere the history or a plan would lose its name if the exercise disappeared. */
    fun isUsed(id: String, workouts: List<Workout>, routines: List<Routine>, active: Workout?): Boolean =
        workouts.any { w -> w.exercises.any { it.exerciseId == id } } ||
            routines.any { r -> r.items.any { it.exerciseId == id } } ||
            active?.exercises?.any { it.exerciseId == id } == true
}
