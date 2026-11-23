package de.knurl.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.data.Sex
import de.knurl.data.TrainingData
import de.knurl.data.nutrition.NutritionData
import de.knurl.data.nutrition.NutritionRepository
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.Nutrition
import de.knurl.domain.model.FoodEntry
import de.knurl.domain.model.NutritionGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Year

data class NutritionState(
    val day: LocalDate,
    val isToday: Boolean,
    val entries: List<FoodEntry>,
    val totals: Nutrition.Totals,
    /** Null until a goal is set up and a body weight is known. */
    val targets: Nutrition.Targets?,
    val goal: NutritionGoal?,
    /** Body weight change per week over the last four weeks, null without enough weigh-ins. */
    val trendKgPerWeek: Double?,
    val latestWeightKg: Double?,
)

/** Targets from the goal, the latest body weight, age, sex and training days (null if anything essential is missing). */
fun targetsFor(training: TrainingData): Nutrition.Targets? {
    val goal = training.settings.nutrition ?: return null
    val weight = training.bodyWeights.lastOrNull()?.weightKg ?: return null
    val age = training.settings.birthYear?.let { Year.now().value - it }
    return Nutrition.targets(goal, weight, age, training.settings.sex?.let { it == Sex.MALE }, training.settings.weeklyGoal)
}

class NutritionViewModel(training: TrainingRepository, private val nutrition: NutritionRepository) : ViewModel() {

    private val day = MutableStateFlow(LocalDate.now())

    val state: StateFlow<NutritionState?> = combine(training.data.filterNotNull(), nutrition.data.filterNotNull(), day) { t, n, d ->
        build(t, n, d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(t: TrainingData, n: NutritionData, d: LocalDate): NutritionState {
        val entries = n.entries.filter { it.day == d.toString() }.sortedBy { it.atMs }
        return NutritionState(
            day = d,
            isToday = d == LocalDate.now(),
            entries = entries,
            totals = Nutrition.totals(entries),
            targets = targetsFor(t),
            goal = t.settings.nutrition,
            trendKgPerWeek = Nutrition.weeklyTrend(t.bodyWeights, System.currentTimeMillis()),
            latestWeightKg = t.bodyWeights.lastOrNull()?.weightKg,
        )
    }

    /** One day back or forward; never into the future. */
    fun shiftDay(by: Long) = day.update { (it.plusDays(by)).coerceAtMost(LocalDate.now()) }

    fun delete(entry: FoodEntry) = nutrition.delete(entry.id)

    fun restore(entry: FoodEntry) = nutrition.log(entry)

    /** The same food again, now (or on the shown day). */
    fun again(entry: FoodEntry, onDay: LocalDate) =
        nutrition.log(entry.copy(id = nutrition.newId(), day = onDay.toString(), atMs = System.currentTimeMillis()))
}
