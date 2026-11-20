package de.knurl.data.repository

import de.knurl.domain.model.SetEntry
import de.knurl.domain.updateSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingRepositoryTest {

    private val store = InMemoryTrainingStore()
    private val uid = MutableStateFlow<String?>("alice")

    private fun TestScope.repository() = TrainingRepository(
        store = store,
        uid = uid,
        scope = backgroundScope,
        german = { true },
        now = { 1_000L },
        newId = { "w1" },
    )

    @Test fun newUserGetsStarterRoutines() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        assertEquals(4, repo.data.value!!.routines.size)
        assertTrue(store.users.getValue("alice").profileCreated)
    }

    @Test fun editsToTheRunningWorkoutSurviveOtherChanges() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.startWorkout(repo.data.value!!.routines.first(), "Training")
        repo.updateActive { it.updateSet(0, 0) { s -> s.copy(weightKg = 100.0) } }

        // A settings change arrives from the store; it must not bring back an older running workout.
        repo.updateSettings { it.copy(weeklyGoal = 5) }

        val active = repo.data.value!!.active
        assertNotNull(active)
        assertEquals(100.0, active!!.exercises[0].sets[0].weightKg, 0.0)
        assertEquals(5, repo.data.value!!.settings.weeklyGoal)
    }

    @Test fun finishingSavesTickedSetsAndClearsTheRunningWorkout() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.startWorkout(repo.data.value!!.routines.first(), "Training")
        repo.updateActive { it.updateSet(0, 0) { s -> s.copy(weightKg = 60.0, reps = 5, done = true) } }

        repo.finishActive()

        val data = repo.data.value!!
        assertNull(data.active)
        assertEquals(listOf(SetEntry(weightKg = 60.0, reps = 5, done = true)), data.workouts.single().exercises.single().sets)
    }

    @Test fun switchingAccountsShowsTheOtherUsersData() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.updateSettings { it.copy(weeklyGoal = 6) }
        uid.value = "bob"
        assertEquals(3, repo.data.value!!.settings.weeklyGoal)
        uid.value = null
        assertNull(repo.data.value)
    }

    @Test fun aChangeFromAnotherDeviceReplacesTheRunningWorkout() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.startWorkout(null, "Training")
        val server = store.users.getValue("alice").active
        server.value = repo.data.value!!.active // the server confirms our write
        server.value = null // then the workout is finished on the other phone
        assertNull(repo.data.value!!.active)
    }
}
