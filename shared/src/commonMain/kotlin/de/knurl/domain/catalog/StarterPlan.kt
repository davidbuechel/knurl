package de.knurl.domain.catalog

import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem
import kotlinx.serialization.Serializable

@Serializable
enum class Goal { MUSCLE, STRENGTH, FITNESS }

@Serializable
enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }

/**
 * The routines a new person starts with, from three answers. 2–3 days: full body A/B;
 * 4 days: upper/lower; 5–6 days: push/pull/legs. Goal sets reps and rest, experience the sets.
 */
object StarterPlan {

    private enum class Slot { MAIN, ACCESSORY, ISOLATION }

    private data class Pick(val exerciseId: String, val slot: Slot)

    fun routines(goal: Goal, experience: Experience, daysPerWeek: Int, german: Boolean): List<Routine> {
        val days = daysPerWeek.coerceIn(1, 7)
        val templates: List<Pair<String, List<Pick>>> = when {
            days <= 3 -> listOf(
                (if (german) "Ganzkörper A" else "Full body A") to listOf(
                    Pick("back_squat", Slot.MAIN), Pick("bench_press", Slot.MAIN), Pick("lat_pulldown", Slot.ACCESSORY),
                    Pick("romanian_deadlift", Slot.ACCESSORY), Pick("lateral_raise", Slot.ISOLATION), Pick("cable_crunch", Slot.ISOLATION),
                ),
                (if (german) "Ganzkörper B" else "Full body B") to listOf(
                    Pick("deadlift", Slot.MAIN), Pick("overhead_press", Slot.MAIN), Pick("seated_cable_row", Slot.ACCESSORY),
                    Pick("leg_press", Slot.ACCESSORY), Pick("db_curl", Slot.ISOLATION), Pick("triceps_pushdown", Slot.ISOLATION),
                ),
            )
            days == 4 -> listOf(
                (if (german) "Oberkörper" else "Upper") to listOf(
                    Pick("bench_press", Slot.MAIN), Pick("barbell_row", Slot.MAIN), Pick("db_shoulder_press", Slot.ACCESSORY),
                    Pick("lat_pulldown", Slot.ACCESSORY), Pick("lateral_raise", Slot.ISOLATION), Pick("ez_curl", Slot.ISOLATION),
                    Pick("triceps_pushdown", Slot.ISOLATION),
                ),
                (if (german) "Unterkörper" else "Lower") to listOf(
                    Pick("back_squat", Slot.MAIN), Pick("romanian_deadlift", Slot.MAIN), Pick("leg_press", Slot.ACCESSORY),
                    Pick("lying_leg_curl", Slot.ISOLATION), Pick("standing_calf_raise", Slot.ISOLATION), Pick("hanging_leg_raise", Slot.ISOLATION),
                ),
            )
            else -> listOf(
                "Push" to listOf(
                    Pick("bench_press", Slot.MAIN), Pick("db_incline_press", Slot.ACCESSORY), Pick("db_shoulder_press", Slot.ACCESSORY),
                    Pick("lateral_raise", Slot.ISOLATION), Pick("triceps_pushdown", Slot.ISOLATION),
                ),
                "Pull" to listOf(
                    Pick("barbell_row", Slot.MAIN), Pick("lat_pulldown", Slot.ACCESSORY), Pick("seated_cable_row", Slot.ACCESSORY),
                    Pick("face_pull", Slot.ISOLATION), Pick("ez_curl", Slot.ISOLATION),
                ),
                (if (german) "Beine" else "Legs") to listOf(
                    Pick("back_squat", Slot.MAIN), Pick("romanian_deadlift", Slot.ACCESSORY), Pick("leg_press", Slot.ACCESSORY),
                    Pick("lying_leg_curl", Slot.ISOLATION), Pick("standing_calf_raise", Slot.ISOLATION),
                ),
            )
        }
        return templates.mapIndexed { index, (name, picks) ->
            Routine(
                id = "starter_${index + 1}",
                name = name,
                position = index,
                items = picks.map { RoutineItem(it.exerciseId, sets(it.slot, experience), reps(it.slot, goal), rest(it.slot, goal)) },
            )
        }
    }

    private fun sets(slot: Slot, experience: Experience): Int = when (experience) {
        Experience.BEGINNER -> if (slot == Slot.ISOLATION) 2 else 3
        Experience.INTERMEDIATE -> 3
        Experience.ADVANCED -> if (slot == Slot.MAIN) 4 else 3
    }

    private fun reps(slot: Slot, goal: Goal): RepRange = when (goal) {
        Goal.STRENGTH -> when (slot) { Slot.MAIN -> RepRange(3, 6); Slot.ACCESSORY -> RepRange(6, 10); Slot.ISOLATION -> RepRange(8, 12) }
        Goal.MUSCLE -> when (slot) { Slot.MAIN -> RepRange(6, 10); Slot.ACCESSORY -> RepRange(8, 12); Slot.ISOLATION -> RepRange(12, 15) }
        Goal.FITNESS -> when (slot) { Slot.MAIN -> RepRange(10, 15); else -> RepRange(12, 15) }
    }

    private fun rest(slot: Slot, goal: Goal): Int = when (goal) {
        Goal.STRENGTH -> when (slot) { Slot.MAIN -> 180; Slot.ACCESSORY -> 120; Slot.ISOLATION -> 90 }
        Goal.MUSCLE -> when (slot) { Slot.MAIN -> 150; Slot.ACCESSORY -> 90; Slot.ISOLATION -> 60 }
        Goal.FITNESS -> if (slot == Slot.MAIN) 75 else 45
    }
}
