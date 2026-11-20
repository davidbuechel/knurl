package de.knurl.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.HardSets
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Volume
import de.knurl.domain.calc.Weeks
import de.knurl.domain.model.Muscle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock

data class ProgressState(
    val sessions: Int,
    val volumeKg: Double,
    val records: Int,
    /** Hard sets this week for every muscle, in body order, including zeros. */
    val hardSets: List<Pair<Muscle, Double>>,
    val streakWeeks: Int,
    val weeklyGoal: Int,
    /** Volume of the last eight weeks, oldest first, the current week last. */
    val weeklyVolumeKg: List<Double>,
)

class ProgressViewModel(private val training: TrainingRepository) : ViewModel() {

    val state: StateFlow<ProgressState?> = training.data.filterNotNull()
        .map { data ->
            val zone = TimeZone.currentSystemDefault()
            val today = Weeks.dateOf(Clock.System.now().toEpochMilliseconds(), zone)
            val weekStart = Weeks.startOf(today)
            val week = Weeks.inWeek(data.workouts, weekStart, zone)
            val ids = week.map { it.id }.toSet()
            val perMuscle = HardSets.perMuscle(week, training::exercise)
            ProgressState(
                sessions = week.size,
                volumeKg = week.sumOf(Volume::of),
                records = History.records(data.workouts).count { it.workoutId in ids },
                hardSets = Muscle.entries.map { it to (perMuscle[it] ?: 0.0) },
                streakWeeks = Weeks.streak(data.workouts, today, zone, data.settings.weeklyGoal),
                weeklyGoal = data.settings.weeklyGoal,
                weeklyVolumeKg = (7 downTo 0).map { back ->
                    Weeks.inWeek(data.workouts, weekStart.minus(back * 7, DateTimeUnit.DAY), zone).sumOf(Volume::of)
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
