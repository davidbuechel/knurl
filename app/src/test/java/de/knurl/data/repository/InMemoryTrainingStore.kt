package de.knurl.data.repository

import de.knurl.data.Settings
import de.knurl.data.TrainingData
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A store that keeps everything in memory and answers at once, like Firestore's local cache. */
class InMemoryTrainingStore : TrainingStore {
    val users = mutableMapOf<String, InMemoryUserStore>()
    override fun open(uid: String): UserTrainingStore = users.getOrPut(uid) { InMemoryUserStore() }
}

class InMemoryUserStore : UserTrainingStore {
    val state = MutableStateFlow(TrainingData())
    val active = MutableStateFlow<Workout?>(null)
    var profileCreated = false
    var activeWrites = 0

    override val data: Flow<TrainingData> = state.map { it.copy(active = null) }
    override val activeChanges: Flow<Workout?> = active

    override suspend fun ensureProfile(starter: List<Routine>, settings: Settings) {
        if (profileCreated) return
        profileCreated = true
        state.value = state.value.copy(routines = starter, settings = settings)
    }

    override fun saveActive(workout: Workout?) {
        activeWrites++
        // Like Firestore, our own write is not echoed back as a remote change.
    }

    override fun finishActive(saved: Workout?) {
        if (saved != null) state.value = state.value.copy(workouts = state.value.workouts + saved)
    }

    override fun saveWorkout(workout: Workout) {
        state.value = state.value.copy(workouts = state.value.workouts.filterNot { it.id == workout.id } + workout)
    }

    override fun deleteWorkout(id: String) {
        state.value = state.value.copy(workouts = state.value.workouts.filterNot { it.id == id })
    }

    override fun saveRoutine(routine: Routine) {
        state.value = state.value.copy(routines = state.value.routines.filterNot { it.id == routine.id } + routine)
    }

    override fun deleteRoutine(id: String) {
        state.value = state.value.copy(routines = state.value.routines.filterNot { it.id == id })
    }

    override fun saveSettings(settings: Settings) {
        state.value = state.value.copy(settings = settings)
    }

    override fun saveBodyWeight(entry: BodyWeight) {
        state.value = state.value.copy(bodyWeights = (state.value.bodyWeights.filterNot { it.id == entry.id } + entry).sortedBy { it.atMs })
    }

    override fun deleteBodyWeight(id: String) {
        state.value = state.value.copy(bodyWeights = state.value.bodyWeights.filterNot { it.id == id })
    }

    override suspend fun deleteEverything() {
        profileCreated = false
        active.value = null
        state.value = TrainingData()
    }
}
