package de.knurl.domain.calc

import de.knurl.domain.model.BodyWeight
import de.knurl.domain.model.Food
import de.knurl.domain.model.FoodEntry
import de.knurl.domain.model.NutritionGoal
import de.knurl.domain.model.WeightGoal
import kotlin.math.roundToInt

/**
 * Daily targets and day totals. Estimates on purpose: good enough to steer by, corrected by the
 * body-weight trend over the weeks, not precise to the calorie.
 */
object Nutrition {

    data class Targets(val kcal: Int, val proteinG: Int)

    data class Totals(val kcal: Int, val proteinG: Double, val carbsG: Double, val fatG: Double)

    /**
     * Energy: Mifflin-St Jeor when height, age and sex are known, otherwise 22 kcal per kg body
     * weight; times an activity factor from the training days per week; then -20 % (at most
     * 600 kcal) to lose, +10 % to gain. Protein: 2.0 g/kg to lose, 1.8 to gain, 1.6 to keep, counting
     * at most 120 kg.
     * Values set by hand in [goal] win. Rounded to 50 kcal and 5 g.
     */
    fun targets(goal: NutritionGoal, weightKg: Double, age: Int?, male: Boolean?, trainingDaysPerWeek: Int): Targets {
        val height = goal.heightCm
        val bmr = if (height != null && age != null && male != null) {
            10 * weightKg + 6.25 * height - 5 * age + if (male) 5 else -161
        } else {
            22 * weightKg
        }
        val activity = when {
            trainingDaysPerWeek <= 2 -> 1.375
            trainingDaysPerWeek <= 4 -> 1.5
            else -> 1.65
        }
        val maintenance = bmr * activity
        val kcal = when (goal.goal) {
            WeightGoal.LOSE -> maintenance - minOf(maintenance * 0.2, 600.0)
            WeightGoal.MAINTAIN -> maintenance
            WeightGoal.GAIN -> maintenance * 1.1
        }
        val proteinPerKg = when (goal.goal) {
            WeightGoal.LOSE -> 2.0
            WeightGoal.MAINTAIN -> 1.6
            WeightGoal.GAIN -> 1.8
        }
        return Targets(
            kcal = goal.kcalOverride ?: ((kcal / 50).roundToInt() * 50),
            // Above 120 kg body weight more protein per kilo is no longer useful (much of it is not muscle).
            proteinG = goal.proteinOverride ?: ((minOf(weightKg, 120.0) * proteinPerKg / 5).roundToInt() * 5),
        )
    }

    /** What [grams] of [food] add up to. */
    fun portion(food: Food, grams: Double, id: String, day: String, atMs: Long): FoodEntry {
        val f = grams / 100
        return FoodEntry(
            id = id, day = day, atMs = atMs,
            name = if (food.brand.isBlank()) food.name else "${food.name} (${food.brand})",
            kcal = (food.kcalPer100 * f).roundToInt(),
            proteinG = round1(food.proteinPer100 * f),
            carbsG = food.carbsPer100?.let { round1(it * f) },
            fatG = food.fatPer100?.let { round1(it * f) },
            grams = grams,
            foodId = food.id,
        )
    }

    fun totals(entries: List<FoodEntry>): Totals = Totals(
        kcal = entries.sumOf { it.kcal },
        proteinG = round1(entries.sumOf { it.proteinG }),
        carbsG = round1(entries.sumOf { it.carbsG ?: 0.0 }),
        fatG = round1(entries.sumOf { it.fatG ?: 0.0 }),
    )

    /**
     * Body weight change per week over the last [days] (least squares), from at least three
     * entries spanning a week; null otherwise.
     */
    fun weeklyTrend(weights: List<BodyWeight>, nowMs: Long, days: Int = 28): Double? {
        val since = nowMs - days * Progress.DAY_MS
        val recent = weights.filter { it.atMs in since..nowMs }
        if (recent.size < 3 || recent.maxOf { it.atMs } - recent.minOf { it.atMs } < 7 * Progress.DAY_MS) return null
        val xs = recent.map { (it.atMs - since).toDouble() / (7 * Progress.DAY_MS) }
        val ys = recent.map { it.weightKg }
        val mx = xs.average()
        val my = ys.average()
        val denominator = xs.sumOf { (it - mx) * (it - mx) }
        if (denominator == 0.0) return null
        return round1(xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / denominator)
    }

    private fun round1(value: Double) = (value * 10).roundToInt() / 10.0
}
