package de.knurl.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.TrainingData
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.Change
import de.knurl.domain.calc.Estimates
import de.knurl.domain.calc.Improvement
import de.knurl.domain.calc.Period
import de.knurl.domain.calc.Periods
import de.knurl.domain.calc.Progress
import de.knurl.domain.calc.History
import de.knurl.domain.calc.PersonalRecord
import de.knurl.domain.calc.Volume
import de.knurl.domain.calc.Weeks
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlin.time.Clock

data class NextUp(
    val routine: Routine,
    val position: Int,
    val of: Int,
    val minutes: Int,
    /** Days since this routine was last done, null if never. */
    val daysAgo: Int?,
)

data class RecordRow(val record: PersonalRecord, val exercise: Exercise?)

data class GainRow(val improvement: Improvement, val exercise: Exercise?)

data class TodayState(
    val active: Workout?,
    val next: NextUp?,
    /** Monday to Sunday of this week. */
    val weekTrained: List<Boolean>,
    val todayIndex: Int,
    val weekSessions: Int,
    val weeklyGoal: Int,
    val weekVolumeKg: Double,
    /** Volume this week against last week up to the same day, in percent. */
    val weekVolumeChange: Int?,
    /** Biggest gains of the last four weeks. */
    val gains: List<GainRow>,
    val streakWeeks: Int,
    val recentRecords: List<RecordRow>,
    val hour: Int,
)

class TodayViewModel(private val training: TrainingRepository) : ViewModel() {

    val state: StateFlow<TodayState?> = training.data.filterNotNull()
        .map(::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startRoutine(routine: Routine, freeName: String) = training.startWorkout(routine, freeName)

    fun startFree(freeName: String) = training.startWorkout(null, freeName)

    private fun build(data: TrainingData): TodayState {
        val zone = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Weeks.dateOf(nowMs, zone)
        val weekStart = Weeks.startOf(today)
        val week = Weeks.inWeek(data.workouts, weekStart, zone)
        val trainedDays = week.map { Weeks.dateOf(it.startedAtMs, zone) }.toSet()
        return TodayState(
            active = data.active,
            next = nextUp(data, today),
            weekTrained = (0 until 7).map { weekStart.plus(it, DateTimeUnit.DAY) in trainedDays },
            todayIndex = today.dayOfWeek.isoDayNumber - 1,
            weekSessions = week.size,
            weeklyGoal = data.settings.weeklyGoal,
            weekVolumeKg = week.sumOf(Volume::of),
            weekVolumeChange = Change(week.sumOf(Volume::of), Periods.inRange(data.workouts, Periods.previousSoFar(Period.WEEK, today), zone).sumOf(Volume::of)).percent,
            gains = Progress.topImprovers(data.workouts, nowMs, days = 28).map { GainRow(it, training.exercise(it.exerciseId)) },
            streakWeeks = Weeks.streak(data.workouts, today, zone, data.settings.weeklyGoal),
            recentRecords = History.records(data.workouts).takeLast(4).reversed().map { RecordRow(it, training.exercise(it.exerciseId)) },
            hour = java.time.LocalTime.now().hour,
        )
    }

    /** The routine after the one trained last, so a split runs in order by itself. */
    private fun nextUp(data: TrainingData, today: kotlinx.datetime.LocalDate): NextUp? {
        val routines = data.routines.filter { it.items.isNotEmpty() }
        if (routines.isEmpty()) return null
        val lastId = data.workouts.filter { it.routineId != null }.maxByOrNull { it.startedAtMs }?.routineId
        val index = (routines.indexOfFirst { it.id == lastId } + 1) % routines.size
        val routine = routines[index]
        val zone = TimeZone.currentSystemDefault()
        val lastDone = data.workouts.filter { it.routineId == routine.id }.maxOfOrNull { it.startedAtMs }
        return NextUp(
            routine = routine,
            position = index + 1,
            of = routines.size,
            minutes = Estimates.minutes(routine),
            daysAgo = lastDone?.let { Weeks.dateOf(it, zone).daysUntil(today) },
        )
    }
}
