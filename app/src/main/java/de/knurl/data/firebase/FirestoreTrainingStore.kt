package de.knurl.data.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import de.knurl.data.Settings
import de.knurl.data.TrainingData
import de.knurl.data.repository.TrainingStore
import de.knurl.data.repository.UserTrainingStore
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.KSerializer

/**
 * Data model (see docs/firebase.md and firestore.rules):
 * users/{uid}                      settings, createdAt, schemaVersion
 * users/{uid}/workouts/{id}        finished workouts
 * users/{uid}/routines/{id}        routines
 * users/{uid}/customExercises/{id} the user's own exercises
 * users/{uid}/state/active         the running workout, deleted when it ends
 */
class FirestoreTrainingStore(private val db: FirebaseFirestore) : TrainingStore {
    override fun open(uid: String): UserTrainingStore = FirestoreUserStore(db.collection("users").document(uid))
}

private class FirestoreUserStore(private val user: DocumentReference) : UserTrainingStore {

    private val workouts = user.collection("workouts")
    private val routines = user.collection("routines")
    private val customExercises = user.collection("customExercises")
    private val active = user.collection("state").document("active")

    override val data: Flow<TrainingData> = combine(
        settingsFlow(),
        workouts.listFlow(Workout.serializer()),
        routines.listFlow(Routine.serializer()),
        customExercises.listFlow(Exercise.serializer()),
    ) { settings, workouts, routines, custom ->
        TrainingData(
            workouts = workouts.sortedBy { it.startedAtMs },
            routines = routines.sortedWith(compareBy<Routine> { it.position }.thenBy { it.name }),
            customExercises = custom,
            settings = settings,
        )
    }

    override val activeChanges: Flow<Workout?> = activeFlow()

    override suspend fun ensureProfile(starter: List<Routine>, settings: Settings) {
        if (user.get().await().exists()) return
        val batch = user.firestore.batch()
        batch.set(user, mapOf(
            "settings" to FirestoreJson.encode(Settings.serializer(), settings),
            "schemaVersion" to SCHEMA_VERSION,
            "createdAt" to FieldValue.serverTimestamp(),
        ))
        starter.forEach { batch.set(routines.document(it.id), withUpdatedAt(Routine.serializer(), it)) }
        batch.commit().await()
    }

    override fun saveActive(workout: Workout?) {
        if (workout == null) active.delete().logFailure("delete active")
        else active.set(withUpdatedAt(Workout.serializer(), workout)).logFailure("save active")
    }

    override fun finishActive(saved: Workout?) {
        val batch = user.firestore.batch()
        if (saved != null) batch.set(workouts.document(saved.id), withUpdatedAt(Workout.serializer(), saved))
        batch.delete(active)
        batch.commit().logFailure("finish workout")
    }

    override fun deleteWorkout(id: String) {
        workouts.document(id).delete().logFailure("delete workout")
    }

    override fun saveRoutine(routine: Routine) {
        routines.document(routine.id).set(withUpdatedAt(Routine.serializer(), routine)).logFailure("save routine")
    }

    override fun deleteRoutine(id: String) {
        routines.document(id).delete().logFailure("delete routine")
    }

    override fun saveSettings(settings: Settings) {
        user.set(mapOf("settings" to FirestoreJson.encode(Settings.serializer(), settings)), SetOptions.merge())
            .logFailure("save settings")
    }

    override suspend fun deleteEverything() {
        // Firestore has no recursive delete on clients: remove every known subcollection, then the profile.
        for (collection in listOf(workouts, routines, customExercises, user.collection("state"), user.collection("devices"))) {
            collection.get().await().documents.chunked(400).forEach { chunk ->
                val batch = user.firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        user.delete().await()
    }

    private fun settingsFlow(): Flow<Settings> = callbackFlow {
        val registration = user.addSnapshotListener { snapshot, error ->
            if (error != null) { Log.w(TAG, "settings", error); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val map = snapshot?.get("settings") as? Map<String, Any?>
            trySend(map?.let { runCatching { FirestoreJson.decode(Settings.serializer(), it) }.getOrNull() } ?: Settings())
        }
        awaitClose { registration.remove() }
    }

    /**
     * The running workout. Later snapshots that still contain our own unconfirmed writes are skipped:
     * the repository applied those already, and replaying them could undo a newer tap.
     */
    private fun activeFlow(): Flow<Workout?> = callbackFlow {
        // The first snapshot always counts: after a restart offline, queued writes are all we have.
        var first = true
        val registration = active.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) { Log.w(TAG, "active", error); return@addSnapshotListener }
            if (snapshot == null || (!first && snapshot.metadata.hasPendingWrites())) return@addSnapshotListener
            first = false
            trySend(snapshot.data?.let { runCatching { FirestoreJson.decode(Workout.serializer(), it) }.getOrNull() })
        }
        awaitClose { registration.remove() }
    }

    private fun <T> CollectionReference.listFlow(serializer: KSerializer<T>): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) { Log.w(TAG, path, error); return@addSnapshotListener }
            val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.data?.let { data ->
                    runCatching { FirestoreJson.decode(serializer, data) }
                        .onFailure { Log.w(TAG, "Skipping unreadable ${doc.reference.path}", it) }
                        .getOrNull()
                }
            }
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    private fun <T> withUpdatedAt(serializer: KSerializer<T>, value: T): Map<String, Any?> =
        FirestoreJson.encode(serializer, value) + ("updatedAt" to FieldValue.serverTimestamp())

    private fun Task<*>.logFailure(what: String) {
        addOnFailureListener { Log.e(TAG, "Could not $what", it) }
    }

    private companion object {
        const val TAG = "FirestoreStore"
        const val SCHEMA_VERSION = 1
    }
}
