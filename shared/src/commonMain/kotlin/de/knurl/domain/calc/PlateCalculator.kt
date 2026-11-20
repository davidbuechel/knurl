package de.knurl.domain.calc

/** Which plates go on each side of the bar. Works in grams internally so 1.25 kg plates add up exactly. */
object PlateCalculator {

    val standardPlatesKg: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    /** Usual plates in pound gyms. [load] works in any unit as long as all numbers share it. */
    val standardPlatesLb: List<Double> = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    data class Load(
        /** Plates for one side, heaviest first. */
        val perSide: List<Double>,
        /** Weight that could not be made with the available plates (total, both sides). */
        val missingKg: Double,
    ) {
        val isExact: Boolean get() = missingKg == 0.0
    }

    fun load(targetKg: Double, barKg: Double = 20.0, platesKg: List<Double> = standardPlatesKg): Load {
        val perSideGrams = ((targetKg - barKg) * 1000 / 2).toLong()
        if (perSideGrams <= 0) return Load(emptyList(), 0.0)

        var left = perSideGrams
        val result = mutableListOf<Double>()
        for (plate in platesKg.sortedDescending()) {
            val grams = (plate * 1000).toLong()
            if (grams <= 0) continue
            while (left >= grams) {
                result += plate
                left -= grams
            }
        }
        return Load(result, left * 2 / 1000.0)
    }
}
