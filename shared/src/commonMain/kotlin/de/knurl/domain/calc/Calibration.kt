package de.knurl.domain.calc

import kotlin.math.ceil
import kotlin.math.floor

/** How a set felt: the one question the app asks while it learns a new exercise. */
enum class Feel { EASY, RIGHT, HARD }

/**
 * Finding the working weight for an exercise without history: after the first set the person
 * says how it felt, and the next sets move by about 10 %, always by at least one plate step
 * ([incrementKg], e.g. 2.5 kg) and onto a weight the equipment can make.
 */
object Calibration {

    fun next(weightKg: Double, feel: Feel, incrementKg: Double): Double {
        if (weightKg <= 0 || incrementKg <= 0) return weightKg
        return when (feel) {
            Feel.RIGHT -> weightKg
            Feel.EASY -> maxOf(weightKg + incrementKg, ceil(weightKg * 1.1 / incrementKg) * incrementKg)
            Feel.HARD -> maxOf(0.0, minOf(weightKg - incrementKg, floor(weightKg * 0.9 / incrementKg) * incrementKg))
        }
    }
}
