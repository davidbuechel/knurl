package de.knurl.core.health

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import de.knurl.core.settings.AppPreferences
import de.knurl.data.repository.TrainingRepository
import de.knurl.data.repository.WorkoutListener
import de.knurl.domain.calc.Calories
import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Average and highest heart rate during a workout, from a watch via Health Connect. */
data class HeartRate(val averageBpm: Long, val maxBpm: Long)

enum class HealthStatus { UNAVAILABLE, NEEDS_INSTALL, AVAILABLE }

/**
 * Health Connect, Android's shared health store: watches and other apps (Fitbit, Samsung Health,
 * Garmin Connect …) meet there. Knurl writes finished workouts with their estimated energy, reads
 * the heart rate during a workout and keeps body weight in step. Everything is opt-in, per switch,
 * and stays on the phone: heart rate is never uploaded (docs/health-connect.md).
 */
class HealthSync(private val context: Context, private val preferences: AppPreferences, private val scope: CoroutineScope) : WorkoutListener {

    lateinit var training: TrainingRepository

    private val _status = MutableStateFlow(status())
    val statusFlow: StateFlow<HealthStatus> = _status.asStateFlow()

    private val _granted = MutableStateFlow<Set<String>>(emptySet())
    val granted: StateFlow<Set<String>> = _granted.asStateFlow()

    private fun status(): HealthStatus = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_INSTALL
        else -> HealthStatus.UNAVAILABLE
    }

    private val client: HealthConnectClient? get() = if (status() == HealthStatus.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    /** Re-reads availability and permissions (they change in system settings) and imports new weights. */
    fun refresh() {
        _status.value = status()
        scope.launch {
            val c = client ?: return@launch
            _granted.value = runCatching { c.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            if (weightOn()) importWeights()
        }
    }

    fun workoutsOn() = preferences.healthWorkouts.value && _granted.value.containsAll(WORKOUT_PERMISSIONS)
    fun heartRateOn() = preferences.healthHeartRate.value && _granted.value.containsAll(HEART_RATE_PERMISSIONS)
    fun weightOn() = preferences.healthWeight.value && _granted.value.containsAll(WEIGHT_PERMISSIONS)

    /** The Play Store page that installs or updates Health Connect. */
    fun installIntent(): Intent = Intent(Intent.ACTION_VIEW, "market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding".toUri())
        .setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Health Connect's own screen, where people see and change what Knurl may do. */
    fun settingsIntent(): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // ---- Workouts

    override fun workoutSaved(workout: Workout, bodyWeights: List<BodyWeight>) {
        if (!workoutsOn()) return
        val end = workout.endedAtMs ?: return
        scope.launch {
            val c = client ?: return@launch
            runCatching {
                val start = Instant.ofEpochMilli(workout.startedAtMs)
                val stop = Instant.ofEpochMilli(end)
                val offset = ZoneId.systemDefault().rules.getOffset(start)
                // A later version with the same client id replaces the record: editing a workout updates it.
                val version = System.currentTimeMillis()
                val conditioning = workout.exercises.count { it.exerciseId in Calories.CONDITIONING_IDS } * 2 > workout.exercises.size
                val records = buildList {
                    add(
                        ExerciseSessionRecord(
                            startTime = start, startZoneOffset = offset, endTime = stop, endZoneOffset = offset,
                            metadata = Metadata.manualEntry(clientRecordId = workout.id, clientRecordVersion = version),
                            exerciseType = if (conditioning) ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
                            else ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                            title = workout.name,
                        ),
                    )
                    Calories.estimate(workout, bodyWeights)?.let { kcal ->
                        add(
                            TotalCaloriesBurnedRecord(
                                startTime = start, startZoneOffset = offset, endTime = stop, endZoneOffset = offset,
                                energy = Energy.kilocalories(kcal.toDouble()),
                                metadata = Metadata.manualEntry(clientRecordId = "${workout.id}-kcal", clientRecordVersion = version),
                            ),
                        )
                    }
                }
                c.insertRecords(records)
            }.onFailure { Log.w(TAG, "Could not write workout", it) }
        }
    }

    override fun workoutDeleted(id: String) {
        if (!workoutsOn()) return
        scope.launch {
            val c = client ?: return@launch
            runCatching {
                c.deleteRecords(ExerciseSessionRecord::class, emptyList(), listOf(id))
                c.deleteRecords(TotalCaloriesBurnedRecord::class, emptyList(), listOf("$id-kcal"))
            }.onFailure { Log.w(TAG, "Could not delete workout", it) }
        }
    }

    /** Heart rate during a workout, or null without permission or data. */
    suspend fun heartRate(workout: Workout): HeartRate? {
        if (!heartRateOn()) return null
        val c = client ?: return null
        val end = workout.endedAtMs ?: return null
        return runCatching {
            val result = c.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX),
                    timeRangeFilter = TimeRangeFilter.between(Instant.ofEpochMilli(workout.startedAtMs), Instant.ofEpochMilli(end)),
                ),
            )
            val avg = result[HeartRateRecord.BPM_AVG] ?: return@runCatching null
            HeartRate(avg, result[HeartRateRecord.BPM_MAX] ?: avg)
        }.onFailure { Log.w(TAG, "Could not read heart rate", it) }.getOrNull()
    }

    // ---- Body weight

    override fun weightAdded(entry: BodyWeight) {
        if (!weightOn()) return
        scope.launch {
            val c = client ?: return@launch
            runCatching {
                val time = Instant.ofEpochMilli(entry.atMs)
                c.insertRecords(
                    listOf(
                        WeightRecord(
                            time = time, zoneOffset = ZoneId.systemDefault().rules.getOffset(time), weight = Mass.kilograms(entry.weightKg),
                            metadata = Metadata.manualEntry(clientRecordId = entry.id),
                        ),
                    ),
                )
            }.onFailure { Log.w(TAG, "Could not write weight", it) }
        }
    }

    override fun weightDeleted(id: String) {
        if (!weightOn() || id.startsWith(IMPORTED)) return
        scope.launch {
            runCatching { client?.deleteRecords(WeightRecord::class, emptyList(), listOf(id)) }
                .onFailure { Log.w(TAG, "Could not delete weight", it) }
        }
    }

    /**
     * Weights from scales and watches of the last 90 days. Each keeps its Health Connect id
     * ("hc-…"), so importing again changes nothing; our own entries are skipped.
     */
    private suspend fun importWeights() {
        val c = client ?: return
        runCatching {
            val since = Instant.now().minusSeconds(90L * 24 * 60 * 60)
            val records = c.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.after(since))).records
            val known = training.data.value?.bodyWeights?.map { it.id }?.toSet() ?: return
            records.filter { it.metadata.dataOrigin.packageName != context.packageName }
                .map { BodyWeight(IMPORTED + it.metadata.id, it.time.toEpochMilli(), it.weight.inKilograms) }
                .filter { it.id !in known }
                .forEach(training::importBodyWeight)
        }.onFailure { Log.w(TAG, "Could not read weights", it) }
    }

    companion object {
        private const val TAG = "HealthSync"
        const val PROVIDER = "com.google.android.apps.healthdata"
        private const val IMPORTED = "hc-"

        val WORKOUT_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        )
        val HEART_RATE_PERMISSIONS = setOf(HealthPermission.getReadPermission(HeartRateRecord::class))
        val WEIGHT_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
        )
    }
}

