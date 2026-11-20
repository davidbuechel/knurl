package de.knurl.core.debug

import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.SetEntry
import de.knurl.domain.model.Workout
import de.knurl.domain.model.WorkoutExercise
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Debug builds only (the Settings row is behind BuildConfig.DEBUG): fills the signed-in test
 * account with 16 weeks of plausible training from its routines, slowly getting stronger, plus
 * weekly body weight. For screenshots and for checking statistics and achievements by hand.
 */
object DemoData {
    private const val WEEKS = 16

    fun fill(training: TrainingRepository) {
        val data = training.data.value ?: return
        val routines = data.routines.filter { it.items.isNotEmpty() }
        if (routines.isEmpty()) return
        val zone = ZoneId.systemDefault()
        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var turn = 0
        for (week in WEEKS downTo 1) {
            val monday = thisMonday.minusWeeks(week.toLong())
            val growth = 1 + (WEEKS - week) * 0.012
            listOf(0L, 2L, 4L).forEachIndexed { slot, offset ->
                // One week in five has a missed session, like real life.
                if (week % 5 == 0 && slot == 2) return@forEachIndexed
                val routine = routines[turn++ % routines.size]
                val start = monday.plusDays(offset).atTime(if (slot == 1) 6 else 18, 30).atZone(zone).toInstant().toEpochMilli()
                val exercises = routine.items.map { item ->
                    val exercise = training.exercise(item.exerciseId)
                    val base = when (exercise?.equipment) {
                        Equipment.BARBELL, Equipment.SMITH -> 60.0
                        Equipment.EZ_BAR -> 30.0
                        Equipment.DUMBBELL -> 18.0
                        Equipment.KETTLEBELL -> 16.0
                        Equipment.MACHINE -> 50.0
                        Equipment.CABLE -> 30.0
                        else -> 0.0
                    }
                    val kg = (base * growth / 2.5).roundToInt() * 2.5
                    WorkoutExercise(
                        item.exerciseId,
                        List(item.sets) { i -> SetEntry(weightKg = kg, reps = (item.repRange.max - i).coerceAtLeast(item.repRange.min), done = true) },
                        item.repRange, item.restSec, group = item.group,
                    )
                }
                val minutes = 45 + (start / 60_000 % 20)
                training.saveWorkout(Workout("demo-$week-$slot", routine.name, routine.id, start, start + minutes * 60_000, exercises))
            }
            val weighIn = monday.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
            training.addBodyWeight(84.0 - (WEEKS - week) * 0.2, weighIn)
        }
    }
}
