package de.knurl.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** When the next reminder of a kind is due. Pure date math, so it is tested in :shared. */
object ReminderTimes {

    /** The next moment after [now] that falls on one of [isoDays] (1 = Monday) at [minutes] after midnight. */
    fun next(now: LocalDateTime, isoDays: Collection<Int>, minutes: Int): LocalDateTime? {
        if (isoDays.isEmpty()) return null
        val time = LocalTime(minutes / 60, minutes % 60)
        for (offset in 0..7) {
            val date = now.date.plus(offset, DateTimeUnit.DAY)
            val candidate = LocalDateTime(date, time)
            if (date.dayOfWeek.isoDayNumber in isoDays && candidate > now) return candidate
        }
        return null
    }

    /** Saturday 17:00, the nudge before a week ends. */
    fun nextStreakCheck(now: LocalDateTime): LocalDateTime = next(now, listOf(6), 17 * 60)!!

    /** Sunday 19:00, the week in review. */
    fun nextWeeklyReview(now: LocalDateTime): LocalDateTime = next(now, listOf(7), 19 * 60)!!
}
