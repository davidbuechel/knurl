package de.knurl.feature.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import de.knurl.R
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.ExerciseLevel
import de.knurl.domain.model.Muscle

@StringRes
fun Muscle.label(): Int = when (this) {
    Muscle.CHEST -> R.string.muscle_chest
    Muscle.BACK -> R.string.muscle_back
    Muscle.SHOULDERS -> R.string.muscle_shoulders
    Muscle.BICEPS -> R.string.muscle_biceps
    Muscle.TRICEPS -> R.string.muscle_triceps
    Muscle.FOREARMS -> R.string.muscle_forearms
    Muscle.QUADS -> R.string.muscle_quads
    Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
    Muscle.GLUTES -> R.string.muscle_glutes
    Muscle.CALVES -> R.string.muscle_calves
    Muscle.CORE -> R.string.muscle_core
}

@StringRes
fun Equipment.label(): Int = when (this) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.EZ_BAR -> R.string.equipment_ez_bar
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.SMITH -> R.string.equipment_smith
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.BAND -> R.string.equipment_band
    Equipment.OTHER -> R.string.equipment_other
}

/** True when the app runs in German; exercise names come from the catalog in both languages. */
@Composable
fun isGerman(): Boolean = LocalConfiguration.current.locales[0].language == "de"

@Composable
fun Exercise.displayName(): String = name(isGerman())

/** "Brust · Langhantel" */
@Composable
fun Exercise.subtitle(): String = "${stringResource(primary.label())} · ${stringResource(equipment.label())}"

@StringRes
fun ExerciseLevel.label(): Int = when (this) {
    ExerciseLevel.BEGINNER -> R.string.level_beginner
    ExerciseLevel.INTERMEDIATE -> R.string.level_intermediate
    ExerciseLevel.EXPERT -> R.string.level_expert
}
