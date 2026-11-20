package de.knurl.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Muscle { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE }

@Serializable
enum class Equipment { BARBELL, EZ_BAR, DUMBBELL, KETTLEBELL, CABLE, MACHINE, SMITH, BODYWEIGHT, BAND, OTHER }

@Serializable
enum class MovementPattern { HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SQUAT, HINGE, LUNGE, ISOLATION, CORE }

@Serializable
enum class ExerciseLevel { BEGINNER, INTERMEDIATE, EXPERT }

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
    val level: ExerciseLevel = ExerciseLevel.BEGINNER,
    /** Step-by-step instructions. German ones only where written so far; the app falls back to English. */
    val instructionsEn: List<String> = emptyList(),
    val instructionsDe: List<String> = emptyList(),
    /** Number of photos served at exercises/{id}/{n}.webp (0, 1 or 2: start and end position). */
    val images: Int = 0,
    /** Own exercises: the person's own cues, shown instead of instructions. */
    val note: String = "",
    /** Own exercises that were removed but still appear in the history keep their name this way. */
    val archived: Boolean = false,
) {
    fun name(german: Boolean): String = if (german) nameDe else nameEn

    fun instructions(german: Boolean): List<String> = if (german && instructionsDe.isNotEmpty()) instructionsDe else instructionsEn
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
