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
    val custom: List<ExerciseRow> = emptyList(),
    val rows: List<ExerciseRow> = emptyList(),
    /** Picker only: the exercises chosen so far, in the order they were tapped. */
    val selected: List<String> = emptyList(),
) {
    val browsing: Boolean get() = query.isBlank() && filter == ExerciseFilter()
}

class ExercisesViewModel(
    private val training: TrainingRepository,
    private val german: Boolean,
    catalogReady: Flow<Boolean>,
    /** Where the filter lives; the picker passes the app-wide one so it is kept between visits. */
    private val filter: MutableStateFlow<ExerciseFilter> = MutableStateFlow(ExerciseFilter()),
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<ExercisesState> = combine(training.data.filterNotNull(), query, filter, catalogReady, selected) { data, q, f, _, chosen ->
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
            favorites = data.settings.favoriteExercises.mapNotNull(training::exercise).filterNot { it.archived }.map(::row),
            recent = recentIds.mapNotNull(training::exercise).filterNot { it.archived }.map(::row),
            custom = data.customExercises.filterNot { it.archived }.sortedBy { it.name(german).lowercase() }.map(::row),
            rows = ordered.map(::row),
            selected = chosen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleMuscle(value: Muscle) = filter.update { it.copy(muscle = if (it.muscle == value) null else value) }
    fun setEquipment(value: Equipment?) = filter.update { it.copy(equipment = value) }
    fun setLevel(value: ExerciseLevel?) = filter.update { it.copy(level = value) }
    fun toggleCustomOnly() = filter.update { it.copy(customOnly = !it.customOnly) }
    /** Picker: tapping an exercise adds it to the selection, tapping it again takes it out. */
    fun toggleSelected(id: String) = selected.update { if (id in it) it - id else it + id }

    /** Picker: an own exercise just created joins the selection. */
    fun select(id: String) = selected.update { if (id in it) it else it + id }

    fun clearSelection() {
        selected.value = emptyList()
    }

    fun clearFilters() {
        filter.value = ExerciseFilter()
    }
}
