package de.knurl.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.ExerciseLevel
import de.knurl.domain.model.Muscle
import de.knurl.domain.search.ExerciseFilter
import de.knurl.domain.search.ExerciseSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ExerciseRow(val exercise: Exercise, val bestOneRepMaxKg: Double?, val favorite: Boolean)

data class ExercisesState(
    val query: String = "",
    val filter: ExerciseFilter = ExerciseFilter(),
    /** Shown above the list while nothing is searched or filtered. */
    val favorites: List<ExerciseRow> = emptyList(),
    val recent: List<ExerciseRow> = emptyList(),
    val rows: List<ExerciseRow> = emptyList(),
) {
    val browsing: Boolean get() = query.isBlank() && filter == ExerciseFilter()
}

class ExercisesViewModel(
    private val training: TrainingRepository,
    private val german: Boolean,
    catalogReady: Flow<Boolean>,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ExerciseFilter())

    val state: StateFlow<ExercisesState> = combine(training.data.filterNotNull(), query, filter, catalogReady) { data, q, f, _ ->
        val trained = data.workouts.flatMap { w -> w.exercises.map { it.exerciseId } }.toSet()
        val best = trained.associateWith { id -> History.sessions(id, data.workouts).mapNotNull { it.bestOneRepMax }.maxOrNull() }
        val favorites = data.settings.favoriteExercises.toSet()
        fun row(e: Exercise) = ExerciseRow(e, best[e.id], e.id in favorites)

        val all = training.allExercises()
        val found = ExerciseSearch.search(all, q, f, german)
        // While browsing, trained exercises come first, then the alphabet.
        val ordered = if (q.isBlank()) found.sortedBy { it.id !in trained } else found
        val recentIds = data.workouts.sortedByDescending { it.startedAtMs }.flatMap { w -> w.exercises.map { it.exerciseId } }.distinct().take(8)
        ExercisesState(
            query = q,
            filter = f,
            favorites = data.settings.favoriteExercises.mapNotNull(training::exercise).map(::row),
            recent = recentIds.mapNotNull(training::exercise).map(::row),
            rows = ordered.map(::row),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleMuscle(value: Muscle) = filter.update { it.copy(muscle = if (it.muscle == value) null else value) }
    fun setEquipment(value: Equipment?) = filter.update { it.copy(equipment = value) }
    fun setLevel(value: ExerciseLevel?) = filter.update { it.copy(level = value) }
    fun clearFilters() {
        filter.value = ExerciseFilter()
    }
}
