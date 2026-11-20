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
    val barKg: Double,
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
            barKg = bar,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changePlateTarget(deltaKg: Double) {
        val current = state.value?.plateTargetKg ?: return
        plateTarget.value = (current + deltaKg).coerceAtLeast(state.value?.barKg ?: 0.0)
    }

    /** Plates in the chosen unit: kilogram plates the gym has, or the usual pound plates. */
    private fun plates(targetKg: Double, barKg: Double, settings: Settings): PlateCalculator.Load =
        if (settings.unit == WeightUnit.LB) {
            PlateCalculator.load(
                targetKg = Math.round(Formatting.toUnit(targetKg, WeightUnit.LB) / 5) * 5.0,
                barKg = Math.round(Formatting.toUnit(barKg, WeightUnit.LB)).toDouble(),
                platesKg = PlateCalculator.standardPlatesLb,
            )
        } else {
            PlateCalculator.load(targetKg, barKg, settings.platesKg)
        }

    private fun Equipment.usesPlates() = this == Equipment.BARBELL || this == Equipment.SMITH || this == Equipment.EZ_BAR
}
