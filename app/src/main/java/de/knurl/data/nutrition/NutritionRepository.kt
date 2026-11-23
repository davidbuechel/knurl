package de.knurl.data.nutrition

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.knurl.data.firebase.FirestoreJson
import de.knurl.domain.model.Food
import de.knurl.domain.model.FoodEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.KSerializer
import java.time.LocalDate
import java.util.UUID

/** What was eaten lately and the foods used before. */
data class NutritionData(
    /** The last [NutritionRepository.DAYS] days, newest first. */
    val entries: List<FoodEntry>,
    /** Most recently used first. */
    val foods: List<Food>,
)

/**
 * The food log and saved foods of the signed-in person, in users/{uid}/foodLog and /foods.
 * Like the training data: Firestore's local cache answers at once and offline, writes are queued.
 */
class NutritionRepository(
    private val db: FirebaseFirestore,
    uid: Flow<String?>,
    scope: CoroutineScope,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val _data = MutableStateFlow<NutritionData?>(null)

    /** Null while signed out or before the first data arrived. */
    val data: StateFlow<NutritionData?> = _data.asStateFlow()

    private var log: CollectionReference? = null
    private var foods: CollectionReference? = null

    init {
        scope.launch {
            uid.collectLatest { id ->
                log = null
                foods = null
                _data.value = null
                if (id == null) return@collectLatest
                val user = db.collection("users").document(id)
                val logRef = user.collection("foodLog").also { log = it }
                val foodsRef = user.collection("foods").also { foods = it }
                val since = LocalDate.now().minusDays(DAYS.toLong()).toString()
                combine(
                    logRef.whereGreaterThanOrEqualTo("day", since).listFlow(FoodEntry.serializer()),
                    foodsRef.orderBy("lastUsedMs", Query.Direction.DESCENDING).limit(200).listFlow(Food.serializer()),
                ) { entries, saved -> NutritionData(entries.sortedByDescending { it.atMs }, saved) }
                    .collect { _data.value = it }
            }
        }
    }

    fun newId(): String = newId.invoke()

    /** The whole food log and all saved foods, for the data export (not just the recent days). */
    suspend fun everything(): NutritionData? {
        val logRef = log ?: return null
        val foodsRef = foods ?: return null
        val entries = logRef.get().await().documents.mapNotNull { d -> d.data?.let { runCatching { FirestoreJson.decode(FoodEntry.serializer(), it) }.getOrNull() } }
        val saved = foodsRef.get().await().documents.mapNotNull { d -> d.data?.let { runCatching { FirestoreJson.decode(Food.serializer(), it) }.getOrNull() } }
        return NutritionData(entries.sortedBy { it.atMs }, saved)
    }

    fun log(entry: FoodEntry) {
        log?.document(entry.id)?.set(stamped(FoodEntry.serializer(), entry))?.addOnFailureListener { Log.w(TAG, "log food", it) }
    }

    fun delete(entryId: String) {
        log?.document(entryId)?.delete()?.addOnFailureListener { Log.w(TAG, "delete food", it) }
    }

    /** Saves a food (or marks it as just used); barcodes double as ids so a pack is stored once. */
    fun saveFood(food: Food) {
        foods?.document(food.id)?.set(stamped(Food.serializer(), food))?.addOnFailureListener { Log.w(TAG, "save food", it) }
    }

    private fun <T> stamped(serializer: KSerializer<T>, value: T): Map<String, Any?> =
        FirestoreJson.encode(serializer, value) + ("updatedAt" to FieldValue.serverTimestamp())

    private fun <T> Query.listFlow(serializer: KSerializer<T>): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) { Log.w(TAG, "listen", error); return@addSnapshotListener }
            trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.data?.let { runCatching { FirestoreJson.decode(serializer, it) }.getOrNull() }
            })
        }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val TAG = "Nutrition"
        /** How far back the log is kept in memory; older days are still in Firestore and in exports. */
        const val DAYS = 120
    }
}
