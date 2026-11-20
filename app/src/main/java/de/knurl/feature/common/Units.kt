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
