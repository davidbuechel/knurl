package de.knurl.domain

import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout

/** Building and changing routines. Pure functions; the editor screen only holds the result. */
object RoutineEditing {

    fun empty(id: String, name: String, position: Int) = Routine(id = id, name = name, items = emptyList(), position = position)

    /**
     * A routine from a workout as it was done: one item per exercise, as many sets as were worked
     * (warm-ups not counted), the rep range and rest the workout used.
     */
    fun fromWorkout(id: String, name: String, workout: Workout, position: Int) = Routine(
        id = id,
        name = name,
        position = position,
        items = workout.exercises.map { item ->
            RoutineItem(
                exerciseId = item.exerciseId,
                sets = item.sets.count { it.kind != SetKind.WARMUP }.coerceAtLeast(1),
                repRange = item.repRange,
                restSec = item.restSec,
                group = item.group,
            )
        },
    )

    /** The routine updated to what was actually done last time, keeping its name and place. */
    fun updateFromWorkout(routine: Routine, workout: Workout) =
        fromWorkout(routine.id, routine.name, workout, routine.position)

    fun duplicate(routine: Routine, id: String, name: String, position: Int) =
        routine.copy(id = id, name = name, position = position)

    fun add(routine: Routine, exerciseId: String, restSec: Int) =
        routine.copy(items = routine.items + RoutineItem(exerciseId, sets = 3, repRange = RepRange.Hypertrophy, restSec = restSec))

    fun remove(routine: Routine, index: Int) = routine.copy(items = routine.items.filterIndexed { i, _ -> i != index })

    /** Moves an item one place up (-1) or down (+1); nothing happens at the ends. */
    fun move(routine: Routine, index: Int, by: Int): Routine {
        val target = index + by
        if (index !in routine.items.indices || target !in routine.items.indices) return routine
        val items = routine.items.toMutableList()
        items.add(target, items.removeAt(index))
        return routine.copy(items = items)
    }

    fun update(routine: Routine, index: Int, transform: (RoutineItem) -> RoutineItem) =
        routine.copy(items = routine.items.mapIndexed { i, item -> if (i == index) transform(item) else item })

    /** Pairs an item with the next one as a superset (or adds the next one to its superset). */
    fun linkWithNext(routine: Routine, index: Int): Routine {
        if (index !in 0 until routine.items.lastIndex) return routine
        val groups = Supersets.link(routine.items.map { it.group }, index)
        return routine.copy(items = routine.items.mapIndexed { i, item -> item.copy(group = groups[i]) })
    }

    /** Takes an item out of its superset. */
    fun unlink(routine: Routine, index: Int): Routine {
        val groups = Supersets.unlink(routine.items.map { it.group }, index)
        return routine.copy(items = routine.items.mapIndexed { i, item -> item.copy(group = groups[i]) })
    }

    /** Positions 0..n-1 in the given order, for the routine list. */
    fun reorder(routines: List<Routine>): List<Routine> = routines.mapIndexed { i, r -> r.copy(position = i) }

    /** Next free position at the end of the list. */
    fun nextPosition(routines: List<Routine>): Int = (routines.maxOfOrNull { it.position } ?: -1) + 1
}
