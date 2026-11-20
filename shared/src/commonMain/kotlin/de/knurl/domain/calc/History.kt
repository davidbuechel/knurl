package de.knurl.domain.calc

import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout

/** One exercise as it was done in one workout. */
data class ExerciseSession(
    val workoutId: String,
    val atMs: Long,
    val sets: List<SetEntry>,
) {
    val bestOneRepMax: Double? get() = OneRepMax.best(sets)
    val volumeKg: Double get() = Volume.of(sets)
    val heaviestKg: Double? get() = sets.filter { it.kind.counts }.maxOfOrNull { it.weightKg }
}

/** A session in which an exercise's estimated 1RM beat every earlier session. */
data class PersonalRecord(
    val exerciseId: String,
    val workoutId: String,
    val atMs: Long,
    val oneRepMaxKg: Double,
    val set: SetEntry,
)

/** Everything derived from finished workouts. Records are never stored by hand; they are always computed here. */
object History {

    /** Sessions of one exercise, oldest first. Unfinished workouts are ignored. */
    fun sessions(exerciseId: String, workouts: Iterable<Workout>): List<ExerciseSession> =
        workouts.filter { it.isFinished }
            .sortedBy { it.startedAtMs }
            .flatMap { workout ->
                workout.exercises.filter { it.exerciseId == exerciseId && it.sets.isNotEmpty() }
                    .map { ExerciseSession(workout.id, workout.startedAtMs, it.sets) }
            }

    /** Sets from the most recent finished session of an exercise, for the "last time" column. */
    fun lastSets(exerciseId: String, workouts: Iterable<Workout>): List<SetEntry> =
        sessions(exerciseId, workouts).lastOrNull()?.sets.orEmpty()

    /**
     * Records over time, oldest first. The first session of an exercise is its baseline, not a record;
     * after that, every session whose best estimated 1RM beats all earlier ones is one record.
     */
    fun records(workouts: Iterable<Workout>): List<PersonalRecord> {
        val best = mutableMapOf<String, Double>()
        val records = mutableListOf<PersonalRecord>()
        for (workout in workouts.filter { it.isFinished }.sortedBy { it.startedAtMs }) {
            for (item in workout.exercises) {
                val (top, value) = item.sets
                    .mapNotNull { set -> OneRepMax.of(set)?.let { set to it } }
                    .maxByOrNull { it.second } ?: continue
                val previous = best[item.exerciseId]
                if (previous == null || value > previous) {
                    best[item.exerciseId] = value
                    if (previous != null) records += PersonalRecord(item.exerciseId, workout.id, workout.startedAtMs, value, top)
                }
            }
        }
        return records
    }
}
