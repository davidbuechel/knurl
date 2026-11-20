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
)
