package de.knurl.core.export

import de.knurl.data.TrainingData
import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExportTest {
    private val data = TrainingData(
        workouts = listOf(
            Workout(
                id = "w1", name = "Push, heavy", startedAtMs = 0, endedAtMs = 1,
                exercises = listOf(WorkoutExercise("bench_press", listOf(SetEntry(SetKind.WARMUP, 60.0, 10), SetEntry(weightKg = 100.0, reps = 5, rir = 2)))),
            ),
        ),
    )

    @Test fun csvHasOneRowPerSetAndQuotesCommas() {
        val lines = DataExport.csv(data, ExerciseCatalog::find).trim().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("\"Push, heavy\",Bench Press,1,warmup,60.0,10,"))
        assertTrue(lines[2].endsWith(",2,working,100.0,5,2"))
    }

    @Test fun jsonContainsEverything() {
        val json = DataExport.json(data)
        assertTrue(json.contains("\"bench_press\""))
        assertTrue(json.contains("\"settings\""))
    }
}
