package de.knurl.domain.model

import kotlinx.serialization.Serializable

/** What the person wants their body weight to do; it sets the calorie target. */
@Serializable
enum class WeightGoal { LOSE, MAINTAIN, GAIN }

/**
 * The nutrition setup (in Settings). The targets are computed (Nutrition.targets) unless set by
 * hand; height is optional and only makes the estimate better.
 */
@Serializable
data class NutritionGoal(
    val goal: WeightGoal = WeightGoal.MAINTAIN,
    val heightCm: Int? = null,
    val kcalOverride: Int? = null,
    val proteinOverride: Int? = null,
)

/**
 * A food used before (scanned, found or typed): values per 100 g, so any amount can be logged.
 * Saved per account and offered again, most recent first.
 */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val brand: String = "",
    val barcode: String? = null,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double? = null,
    val fatPer100: Double? = null,
    /** One portion as printed on the pack, if known. */
    val servingG: Double? = null,
    val lastUsedMs: Long = 0,
)

/** One thing eaten on a [day] (ISO date, the local day). */
@Serializable
data class FoodEntry(
    val id: String,
    val day: String,
    val atMs: Long,
    val name: String,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    /** Amount eaten; null for a quick entry of just calories and protein. */
    val grams: Double? = null,
    val foodId: String? = null,
)
