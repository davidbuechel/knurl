package de.knurl.domain

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderTimesTest {
    // Friday 20 November 2026, 10:00
    private val friday = LocalDateTime(2026, 11, 20, 10, 0)

    @Test fun laterTodayWhenTodayIsADay() =
        assertEquals(LocalDateTime(2026, 11, 20, 18, 0), ReminderTimes.next(friday, listOf(5), 18 * 60))

    @Test fun nextWeekWhenTodaysTimeHasPassed() =
        assertEquals(LocalDateTime(2026, 11, 27, 8, 0), ReminderTimes.next(friday, listOf(5), 8 * 60))

    @Test fun nextChosenDay() =
        assertEquals(LocalDateTime(2026, 11, 23, 18, 0), ReminderTimes.next(friday, listOf(1, 3), 18 * 60))

    @Test fun noDaysNoReminder() = assertNull(ReminderTimes.next(friday, emptyList(), 18 * 60))

    @Test fun weeklyReviewIsSundayEvening() =
        assertEquals(LocalDateTime(2026, 11, 22, 19, 0), ReminderTimes.nextWeeklyReview(friday))
}
