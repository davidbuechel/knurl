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

    /** The decimal separator people expect: "," in German, "." in English. */
    fun decimalSeparator(locale: Locale = Locale.getDefault()): Char = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /** Large counts for small tiles: "850", "3.4k", "12k" (German "3,4k"). */
    fun compact(value: Double, locale: Locale = Locale.getDefault()): String = when {
        value < 1000 -> kg(Math.round(value).toDouble(), locale)
        value < 10_000 -> NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(Math.floor(value / 100) / 10) + "k"
        else -> "${Math.round(value / 1000)}k"
    }

    /** A plain number with up to two decimals. */
    fun kg(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)

    /** The number in the chosen unit; pounds are rounded to a tenth so conversions stay readable. */
    fun weightNumber(kg: Double, unit: WeightUnit, locale: Locale = Locale.getDefault()): String =
        if (unit == WeightUnit.LB) kg(Math.round(kg * LB_PER_KG * 10) / 10.0, locale) else kg(kg, locale)

    /** Joins a number and its unit so a line never breaks between them ("60 kg", not "60 / kg"). */
    const val NBSP = ' '

    /** "102,5 kg" or "226 lb". */
    fun weight(kg: Double, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String =
        "${weightNumber(kg, unit, locale)}$NBSP${symbol(unit)}"

    /** Volume in kilograms below a tonne, in tonnes (one decimal) above; pounds as a whole number. */
    fun tonnage(kgTotal: Double, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String = when {
        unit == WeightUnit.LB -> "${NumberFormat.getIntegerInstance(locale).format((kgTotal * LB_PER_KG).roundToLong())}${NBSP}lb"
        kgTotal < 1000 -> "${kg(Math.round(kgTotal).toDouble(), locale)}${NBSP}kg"
        else -> NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(kgTotal / 1000) + "${NBSP}t"
    }

    /** "100 × 5", or just the reps for bodyweight sets. */
    fun set(weightKg: Double, reps: Int, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String =
        if (weightKg == 0.0) "$reps" else "${weightNumber(weightKg, unit, locale)} × $reps"

    /** "1:24" or "1:02:03". */
    /** How long something took, for lists and summaries: "< 1 min", "55 min", "1 h 5 min". [duration] is for running clocks. */
    fun length(totalSec: Long): String {
        val minutes = totalSec.coerceAtLeast(0) / 60
        return when {
            minutes < 1 -> "< 1 min"
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0L -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }

    /** A whole percentage the local way: "12 %" in German, "12%" in English; [signed] adds "+". */
    fun percent(value: Int, signed: Boolean = false, locale: Locale = Locale.getDefault()): String {
        val number = (if (signed && value > 0) "+" else "") + value
        return if (locale.language == "de") "$number$NBSP%" else "$number%"
    }

    /** A running clock or a rest time: "1:05:30", "2:00". */
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
