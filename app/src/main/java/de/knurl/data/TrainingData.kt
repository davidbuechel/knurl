package de.knurl.data

import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.serialization.Serializable

/** Everything the app keeps on the device. Bump [version] and migrate when the shape changes. */
@Serializable
data class TrainingData(
    val version: Int = 1,
    val workouts: List<Workout> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val customExercises: List<Exercise> = emptyList(),
    /** The workout in progress, kept here so it survives the app being closed mid-session. */
    val active: Workout? = null,
    val settings: Settings = Settings(),
)

@Serializable
data class Settings(
    val barKg: Double = 20.0,
    val defaultRestSec: Int = 120,
    val weeklyGoal: Int = 3,
)
