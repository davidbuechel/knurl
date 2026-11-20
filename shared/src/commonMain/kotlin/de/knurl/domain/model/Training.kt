package de.knurl.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SetKind {
    WARMUP, WORKING, DROP, FAILURE;

    /** Warm-up sets never count towards volume, hard sets or records. */
    val counts: Boolean get() = this != WARMUP
}

/** One set. Weights are always stored in kilograms; the app converts only for display. */
@Serializable
data class SetEntry(
    val kind: SetKind = SetKind.WORKING,
    val weightKg: Double,
    val reps: Int,
    /** Reps in reserve, if the lifter logged it. */
    val rir: Int? = null,
    val done: Boolean = false,
)

@Serializable
data class WorkoutExercise(
    val exerciseId: String,
    val sets: List<SetEntry>,
    val repRange: RepRange = RepRange.Hypertrophy,
    val restSec: Int = 120,
    val note: String = "",
)

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val routineId: String? = null,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    /** How it went, in the person's words. */
    val note: String = "",
) {
    val isFinished: Boolean get() = endedAtMs != null
}

@Serializable
data class RoutineItem(
    val exerciseId: String,
    val sets: Int,
    val repRange: RepRange = RepRange.Hypertrophy,
    val restSec: Int = 120,
)

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val items: List<RoutineItem>,
    /** Order in the routine list; the "up next" suggestion follows it. */
    val position: Int = 0,
)

/** One body weight entry. Kilograms, like every weight. */
@Serializable
data class BodyWeight(
    val id: String,
    val atMs: Long,
    val weightKg: Double,
)
