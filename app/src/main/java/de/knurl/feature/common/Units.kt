package de.knurl.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.knurl.data.WeightUnit
import de.knurl.domain.model.Equipment

/** The unit the person chose (Settings.unit), provided once in KnurlNavHost. */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** "102,5 kg" / "226 lb" in the chosen unit. */
@Composable
fun weightText(kg: Double): String = Formatting.weight(kg, LocalWeightUnit.current)

/** Just the number in the chosen unit, e.g. for an input field. */
@Composable
fun weightNumber(kg: Double): String = Formatting.weightNumber(kg, LocalWeightUnit.current)

/**
 * An estimate (1RM): rounded to what a bar can be loaded with, 0.5 kg or 1 lb. "93,33 kg"
 * would claim a precision the formula does not have.
 */
@Composable
fun estimateText(kg: Double): String {
    val unit = LocalWeightUnit.current
    val inUnit = Formatting.toUnit(kg, unit)
    val rounded = if (unit == WeightUnit.LB) Math.round(inUnit).toDouble() else Math.round(inUnit * 2) / 2.0
    return "${Formatting.kg(rounded)} ${Formatting.symbol(unit)}"
}

@Composable
fun setText(weightKg: Double, reps: Int): String = Formatting.set(weightKg, reps, LocalWeightUnit.current)

@Composable
fun tonnageText(kg: Double): String = Formatting.tonnage(kg, LocalWeightUnit.current)

/**
 * One tap on a weight stepper, in kilograms. In pounds a tap is 5 lb (2.5 lb for small jumps),
 * which is what gyms load; in kilograms it is the equipment's smallest jump.
 */
fun WeightUnit.stepKg(equipment: Equipment?, kgStep: Double): Double = when (this) {
    WeightUnit.KG -> kgStep
    WeightUnit.LB -> Formatting.fromUnit(if (equipment == Equipment.DUMBBELL || equipment == Equipment.KETTLEBELL || kgStep >= 2.5) 5.0 else 2.5, this)
}
