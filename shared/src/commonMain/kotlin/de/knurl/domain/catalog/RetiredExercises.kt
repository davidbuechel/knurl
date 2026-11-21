package de.knurl.domain.catalog

import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout

/**
 * Catalog exercises that left the list (scripts/exercise_list.py) stay in the catalog as archived:
 * old workouts keep their names. Whoever still uses one (history, a routine, the running workout)
 * keeps finding it in the library; everyone else no longer sees it. Own exercises the person
 * deleted stay hidden either way.
 */
object RetiredExercises {

    fun usedIds(workouts: List<Workout>, routines: List<Routine>, active: Workout?): Set<String> =
        (workouts.flatMap { w -> w.exercises.map { it.exerciseId } } +
            routines.flatMap { r -> r.items.map { it.exerciseId } } +
            active?.exercises?.map { it.exerciseId }.orEmpty()).toSet()

    /** [all] with retired catalog exercises from [used] offered again. */
    fun offered(all: List<Exercise>, used: Set<String>): List<Exercise> =
        all.map { if (it.archived && !it.isCustom && it.id in used) it.copy(archived = false) else it }
}
