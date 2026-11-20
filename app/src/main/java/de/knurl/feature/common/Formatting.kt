package de.knurl.feature.common

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** All number and date formatting in one place, in the user's locale ("102,5" in German, "102.5" in English). */
object Formatting {

    fun kg(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value)

    /** Volume in kilograms below a tonne, in tonnes (one decimal) above. */
    fun tonnage(kgTotal: Double, locale: Locale = Locale.getDefault()): String =
        if (kgTotal < 1000) "${kg(Math.round(kgTotal).toDouble(), locale)} kg"
        else NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(kgTotal / 1000) + " t"

    fun set(weightKg: Double, reps: Int, locale: Locale = Locale.getDefault()): String =
        if (weightKg == 0.0) "$reps" else "${kg(weightKg, locale)} × $reps"

    /** "1:24" or "1:02:03". */
    fun duration(totalSec: Long): String {
        val sec = totalSec.coerceAtLeast(0)
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun date(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}
