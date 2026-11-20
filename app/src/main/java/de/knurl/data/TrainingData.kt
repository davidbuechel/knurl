package de.knurl.data

import de.knurl.domain.calc.PlateCalculator
import de.knurl.domain.catalog.Experience
import de.knurl.domain.catalog.Goal
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.serialization.Serializable

/** Everything the screens show for the signed-in user. */
@Serializable
data class TrainingData(
    val version: Int = 1,
    val workouts: List<Workout> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val customExercises: List<Exercise> = emptyList(),
    /** Photos of own exercises: exercise id to a base64 WebP (see exercisePhotos in firestore.rules). */
    val exercisePhotos: Map<String, String> = emptyMap(),
    /** Oldest first. */
    val bodyWeights: List<BodyWeight> = emptyList(),
    /** The workout in progress, kept so it survives the app being closed mid-session. */
    val active: Workout? = null,
    val settings: Settings = Settings(),
)

@Serializable
enum class WeightUnit { KG, LB }

/** Account-wide settings, stored in users/{uid}.settings so they follow the person to a new phone. */
@Serializable
data class Settings(
    val barKg: Double = 20.0,
    val defaultRestSec: Int = 120,
    val weeklyGoal: Int = 3,
    val unit: WeightUnit = WeightUnit.KG,
    /** Plates the person's gym has, for the plate calculator. Always kilograms. */
    val platesKg: List<Double> = PlateCalculator.standardPlatesKg,
    val notifications: NotificationSettings = NotificationSettings(),
    /** Starred exercises, shown first in the library. */
    val favoriteExercises: List<String> = emptyList(),
    /** First-start questions answered (or skipped); stored here so a new phone doesn't ask again. */
    val onboardingDone: Boolean = false,
    val goal: Goal? = null,
    val experience: Experience? = null,
    /** Optional, only for energy from heart rate (Keytel needs age and sex). Never required. */
    val birthYear: Int? = null,
    val sex: Sex? = null,
)

@Serializable
enum class Sex { FEMALE, MALE }

/** Each kind of notification can be switched off on its own; each is its own Android channel too. */
@Serializable
data class NotificationSettings(
    /** "Rest is over" when the app is in the background. */
    val restTimer: Boolean = true,
    /** Ongoing notification with time and rest while a workout runs. */
    val workoutRunning: Boolean = true,
    /** Training reminder on chosen days. Off until the person picks days. */
    val reminders: Boolean = false,
    /** ISO days, 1 = Monday. */
    val reminderDays: List<Int> = listOf(1, 3, 5),
    /** Minutes after midnight. */
    val reminderMinutes: Int = 18 * 60,
    /** Saturday nudge when the weekly goal is in reach and a streak would break. */
    val streak: Boolean = true,
    /** Sunday evening summary of the week. */
    val weeklyReview: Boolean = true,
    /** Product news via push. */
    val news: Boolean = false,
    /** Account and security messages via push. */
    val account: Boolean = true,
)
