package de.knurl.data.repository

import de.knurl.data.Settings
import de.knurl.data.TrainingData
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.flow.Flow

/** Where training data lives. Firestore in the app, in memory in tests. */
interface TrainingStore {
    fun open(uid: String): UserTrainingStore
}

/**
 * One user's data. Writes return immediately and are queued while offline; [data] reflects them
 * at once from the local cache.
 */
interface UserTrainingStore {
    /** Everything for this user except the running workout (always null here). */
    val data: Flow<TrainingData>

    /**
     * The running workout as confirmed by the server, without our own unconfirmed writes. Emits the
     * current value first, then changes made elsewhere (another device) or confirmations.
     */
    val activeChanges: Flow<Workout?>

    /** Creates the profile with starter routines the first time this user is seen. */
    suspend fun ensureProfile(starter: List<Routine>, settings: Settings)

    fun saveActive(workout: Workout?)
    /** Saves the finished workout (if any) and clears the running one in one step. */
    fun finishActive(saved: Workout?)
    fun saveWorkout(workout: Workout)
    fun deleteWorkout(id: String)
    fun saveRoutine(routine: Routine)
    fun deleteRoutine(id: String)
    fun saveSettings(settings: Settings)
    fun saveBodyWeight(entry: BodyWeight)
    fun deleteBodyWeight(id: String)
    fun saveCustomExercise(exercise: Exercise)
    fun deleteCustomExercise(id: String)
    /** [webp] is base64; null removes the photo. */
    fun savePhoto(exerciseId: String, webp: String?)

    /** Deletes every document of this user (account deletion). Needs the network. */
    suspend fun deleteEverything()
}
