package de.knurl.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Muscle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class ExerciseRow(val exercise: Exercise, val bestOneRepMaxKg: Double?)

data class ExercisesState(
    val query: String = "",
    val muscle: Muscle? = null,
    val rows: List<ExerciseRow> = emptyList(),
)

class ExercisesViewModel(private val training: TrainingRepository, private val german: Boolean) : ViewModel() {

    private val query = MutableStateFlow("")
    private val muscle = MutableStateFlow<Muscle?>(null)

    val state: StateFlow<ExercisesState> = combine(training.data.filterNotNull(), query, muscle) { data, q, m ->
        val best = data.workouts.flatMap { w -> w.exercises.map { it.exerciseId } }.toSet()
            .associateWith { id -> History.sessions(id, data.workouts).mapNotNull { it.bestOneRepMax }.maxOrNull() }
        val rows = training.allExercises()
            .filter { m == null || it.primary == m }
            .filter { q.isBlank() || it.name(german).contains(q.trim(), ignoreCase = true) }
            // Exercises the user has trained come first, then alphabetical.
            .sortedWith(compareBy<Exercise> { best[it.id] == null }.thenBy { it.name(german) })
            .map { ExerciseRow(it, best[it.id]) }
        ExercisesState(q, m, rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleMuscle(value: Muscle) {
        muscle.value = if (muscle.value == value) null else value
    }
}
