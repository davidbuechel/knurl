package de.knurl.feature.common

import de.knurl.data.WeightUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

/**
 * All number and date formatting in one place, in the user's locale ("102,5" in German, "102.5" in
 * English). Weights are stored in kilograms; [WeightUnit] only changes what is shown.
 */
object Formatting {

    const val LB_PER_KG = 2.20462262185

    fun toUnit(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) kg * LB_PER_KG else kg

    fun fromUnit(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.LB) value / LB_PER_KG else value

    fun symbol(unit: WeightUnit): String = if (unit == WeightUnit.LB) "lb" else "kg"

    /** A plain number with up to two decimals. */
    fun kg(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)

    /** The number in the chosen unit; pounds are rounded to a tenth so conversions stay readable. */
    fun weightNumber(kg: Double, unit: WeightUnit, locale: Locale = Locale.getDefault()): String =
        if (unit == WeightUnit.LB) kg(Math.round(kg * LB_PER_KG * 10) / 10.0, locale) else kg(kg, locale)

    /** "102,5 kg" or "226 lb". */
    fun weight(kg: Double, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String =
        "${weightNumber(kg, unit, locale)} ${symbol(unit)}"

    /** Volume in kilograms below a tonne, in tonnes (one decimal) above; pounds as a whole number. */
    fun tonnage(kgTotal: Double, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String = when {
        unit == WeightUnit.LB -> "${NumberFormat.getIntegerInstance(locale).format((kgTotal * LB_PER_KG).roundToLong())} lb"
        kgTotal < 1000 -> "${kg(Math.round(kgTotal).toDouble(), locale)} kg"
        else -> NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(kgTotal / 1000) + " t"
    }

    /** "100 × 5", or just the reps for bodyweight sets. */
    fun set(weightKg: Double, reps: Int, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String =
        if (weightKg == 0.0) "$reps" else "${weightNumber(weightKg, unit, locale)} × $reps"

    /** "1:24" or "1:02:03". */
    fun duration(totalSec: Long): String {
        val sec = totalSec.coerceAtLeast(0)
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** "18:30" in the locale's style. */
    fun timeOfDay(minutes: Int, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(java.time.LocalTime.of(minutes / 60, minutes % 60))

    /** "Nov 2026", for places with little room. */
    fun monthYear(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("LLL yyyy", locale).format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    fun date(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}
