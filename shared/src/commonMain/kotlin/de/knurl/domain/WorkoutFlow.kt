package de.knurl.domain

import de.knurl.domain.model.Workout

/** Grouping rules for supersets, on a plain list of group ids so routines and workouts share them. */
object Supersets {

    /** Puts item [index] and the next one into one group (reusing a group either already has). */
    fun link(groups: List<Int?>, index: Int): List<Int?> {
        if (index !in 0 until groups.lastIndex) return groups
        val id = groups[index] ?: groups[index + 1] ?: ((groups.filterNotNull().maxOrNull() ?: 0) + 1)
        val old = groups[index + 1]
        return groups.mapIndexed { i, g ->
            when {
                i == index || i == index + 1 -> id
                old != null && g == old -> id // merging two supersets
                else -> g
            }
        }.let(::tidy)
    }

    /** Takes item [index] out of its group; a group left with one item dissolves. */
    fun unlink(groups: List<Int?>, index: Int): List<Int?> =
        tidy(groups.mapIndexed { i, g -> if (i == index) null else g })

    /** Groups must be neighbours and have at least two members; anything else becomes single. */
    fun tidy(groups: List<Int?>): List<Int?> {
        val result = MutableList<Int?>(groups.size) { null }
        var next = 1
        var i = 0
        while (i < groups.size) {
            val g = groups[i]
            var end = i
            if (g != null) while (end + 1 < groups.size && groups[end + 1] == g) end++
            // each run of two or more equal ids is one superset, numbered 1..n in order
            if (g != null && end > i) {
                for (j in i..end) result[j] = next
                next++
            }
            i = end + 1
        }
        return result
    }

    /** Index ranges of blocks: each single exercise or each superset, in order. */
    fun blocks(groups: List<Int?>): List<IntRange> {
        val blocks = mutableListOf<IntRange>()
        var i = 0
        while (i < groups.size) {
            var end = i
            val g = groups[i]
            if (g != null) while (end + 1 < groups.size && groups[end + 1] == g) end++
            blocks += i..end
            i = end + 1
        }
        return blocks
    }
}

/** A set in a workout: exercise index and set index. */
data class SetRef(val exercise: Int, val set: Int)

/**
 * The order sets are done in: exercise by exercise, but within a superset in rounds
 * (A1, B1, A2, B2, …). The app follows it to move the focus after each ticked set.
 */
object WorkoutFlow {

    fun order(workout: Workout): List<SetRef> {
        val groups = workout.exercises.map { it.group }
        return Supersets.blocks(groups).flatMap { block ->
            val rounds = block.maxOf { workout.exercises[it].sets.size }
            (0 until rounds).flatMap { round ->
                block.filter { round < workout.exercises[it].sets.size }.map { SetRef(it, round) }
            }
        }
    }

    /** The next set not done yet after [after] (wrapping to earlier open sets); null when all are done. */
    fun nextFocus(workout: Workout, after: SetRef?): SetRef? {
        val order = order(workout)
        val open = { ref: SetRef -> !workout.exercises[ref.exercise].sets[ref.set].done }
        val start = after?.let { order.indexOf(it) } ?: -1
        return order.drop(start + 1).firstOrNull(open) ?: order.take(start + 1).firstOrNull(open)
    }

    /**
     * Whether ticking [ref] should start the rest timer: always for a single exercise; in a superset
     * only after the last exercise of the round.
     */
    fun restsAfter(workout: Workout, ref: SetRef): Boolean {
        val groups = workout.exercises.map { it.group }
        val block = Supersets.blocks(groups).first { ref.exercise in it }
        if (block.first == block.last) return true
        val lastInRound = block.last { ref.set < workout.exercises[it].sets.size }
        return ref.exercise == lastInRound
    }

    fun progress(workout: Workout): Pair<Int, Int> =
        workout.exercises.sumOf { e -> e.sets.count { it.done } } to workout.exercises.sumOf { it.sets.size }
}

fun Workout.linkWithNext(exercise: Int): Workout {
    val groups = Supersets.link(exercises.map { it.group }, exercise)
    return copy(exercises = exercises.mapIndexed { i, e -> e.copy(group = groups[i]) })
}

fun Workout.unlink(exercise: Int): Workout {
    val groups = Supersets.unlink(exercises.map { it.group }, exercise)
    return copy(exercises = exercises.mapIndexed { i, e -> e.copy(group = groups[i]) })
}
