package de.knurl.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Muscle { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE }

@Serializable
enum class Equipment { BARBELL, EZ_BAR, DUMBBELL, KETTLEBELL, CABLE, MACHINE, SMITH, BODYWEIGHT, BAND }

@Serializable
enum class MovementPattern { HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SQUAT, HINGE, LUNGE, ISOLATION, CORE }

@Serializable
data class Exercise(
    val id: String,
    val nameDe: String,
    val nameEn: String,
    val primary: Muscle,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Equipment,
    val pattern: MovementPattern,
    val isCustom: Boolean = false,
) {
    fun name(german: Boolean): String = if (german) nameDe else nameEn
}

/** Inclusive target range of repetitions for a working set. */
@Serializable
data class RepRange(val min: Int, val max: Int) {
    init {
        require(min in 1..max) { "Invalid rep range $min..$max" }
    }

    companion object {
        val Hypertrophy = RepRange(8, 12)
    }
}
