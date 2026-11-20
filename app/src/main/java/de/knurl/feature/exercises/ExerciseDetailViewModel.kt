package de.knurl.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.Settings
import de.knurl.data.WeightUnit
import de.knurl.data.repository.TrainingRepository
import de.knurl.feature.common.Formatting
import de.knurl.domain.calc.ExerciseSession
import de.knurl.domain.calc.History
import de.knurl.domain.calc.PlateCalculator
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Equipment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class ExerciseDetailState(
    val exercise: Exercise,
    val sessions: List<ExerciseSession>,
    val bestOneRepMaxKg: Double?,
    val firstOneRepMaxKg: Double?,
    val heaviestKg: Double?,
    val bestVolumeKg: Double?,
    val plateTargetKg: Double,
    val plates: PlateCalculator.Load?,
    /** Target and bar as the plate calculator uses them, in the chosen unit (pounds rounded to 5). */
    val plateTargetInUnit: Double,
    val barInUnit: Double,
    val barKg: Double,
    val favorite: Boolean,
)

class ExerciseDetailViewModel(private val training: TrainingRepository, private val exerciseId: String) : ViewModel() {

    /** The weight the plate calculator shows; starts at the heaviest working weight once it is known. */
    private val plateTarget = MutableStateFlow<Double?>(null)

    val state: StateFlow<ExerciseDetailState?> = combine(training.data.filterNotNull(), plateTarget) { data, target ->
        val exercise = training.exercise(exerciseId) ?: return@combine null
        val sessions = History.sessions(exerciseId, data.workouts)
        val oneRepMaxes = sessions.mapNotNull { it.bestOneRepMax }
        val heaviest = sessions.mapNotNull { it.heaviestKg }.maxOrNull()
        val bar = data.settings.barKg
        val plateWeight = target ?: heaviest ?: (bar + 40.0)
        ExerciseDetailState(
            exercise = exercise,
            sessions = sessions,
            bestOneRepMaxKg = oneRepMaxes.maxOrNull(),
            firstOneRepMaxKg = oneRepMaxes.firstOrNull(),
            heaviestKg = heaviest,
            bestVolumeKg = sessions.maxOfOrNull { it.volumeKg },
            plateTargetKg = plateWeight,
            plates = if (exercise.equipment.usesPlates()) plates(plateWeight, bar, data.settings) else null,
            plateTargetInUnit = inUnit(plateWeight, data.settings.unit, roundTo = 5.0),
            barInUnit = inUnit(bar, data.settings.unit, roundTo = 1.0),
            barKg = bar,
            favorite = exerciseId in data.settings.favoriteExercises,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleFavorite() = training.toggleFavorite(exerciseId)

    fun changePlateTarget(deltaKg: Double) {
        val current = state.value?.plateTargetKg ?: return
        plateTarget.value = (current + deltaKg).coerceAtLeast(state.value?.barKg ?: 0.0)
    }

    /** Plates in the chosen unit: kilogram plates the gym has, or the usual pound plates. */
    private fun plates(targetKg: Double, barKg: Double, settings: Settings): PlateCalculator.Load =
        if (settings.unit == WeightUnit.LB) {
            PlateCalculator.load(
                targetKg = inUnit(targetKg, WeightUnit.LB, roundTo = 5.0),
                barKg = inUnit(barKg, WeightUnit.LB, roundTo = 1.0),
                platesKg = PlateCalculator.standardPlatesLb,
            )
        } else {
            PlateCalculator.load(targetKg, barKg, settings.platesKg)
        }

    /** Kilograms stay as they are; pounds are rounded to what can be loaded. */
    private fun inUnit(kg: Double, unit: WeightUnit, roundTo: Double): Double =
        if (unit == WeightUnit.LB) Math.round(Formatting.toUnit(kg, unit) / roundTo) * roundTo else kg

    private fun Equipment.usesPlates() = this == Equipment.BARBELL || this == Equipment.SMITH || this == Equipment.EZ_BAR
}
