package de.knurl.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.TrainingData
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.PersonalRecord
import de.knurl.domain.calc.Volume
import de.knurl.domain.calc.Weeks
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class TodayState(
    val active: Workout?,
    val nextRoutine: Routine?,
    val weekSessions: Int,
    val weeklyGoal: Int,
    val weekVolumeKg: Double,
    val weekRecords: Int,
    val recentRecords: List<PersonalRecord>,
)

class TodayViewModel(private val training: TrainingRepository) : ViewModel() {

    val state: StateFlow<TodayState?> = training.data.filterNotNull()
        .map(::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startRoutine(routine: Routine, freeName: String) = training.startWorkout(routine, freeName)

    fun startFree(freeName: String) = training.startWorkout(null, freeName)

    fun exerciseName(id: String, german: Boolean) = training.exercise(id)?.name(german) ?: id

    private fun build(data: TrainingData): TodayState {
        val zone = TimeZone.currentSystemDefault()
        val weekStart = Weeks.startOf(Weeks.dateOf(Clock.System.now().toEpochMilliseconds(), zone))
        val week = Weeks.inWeek(data.workouts, weekStart, zone)
        val weekIds = week.map { it.id }.toSet()
        val records = History.records(data.workouts)
        return TodayState(
            active = data.active,
            nextRoutine = nextRoutine(data),
            weekSessions = week.size,
            weeklyGoal = data.settings.weeklyGoal,
            weekVolumeKg = week.sumOf(Volume::of),
            weekRecords = records.count { it.workoutId in weekIds },
            recentRecords = records.takeLast(5).reversed(),
        )
    }

    /** The routine after the one trained last, so a split runs in order by itself. */
    private fun nextRoutine(data: TrainingData): Routine? {
        val routines = data.routines
        if (routines.isEmpty()) return null
        val lastId = data.workouts.filter { it.routineId != null }.maxByOrNull { it.startedAtMs }?.routineId
        val lastIndex = routines.indexOfFirst { it.id == lastId }
        return routines[(lastIndex + 1) % routines.size]
    }
}
