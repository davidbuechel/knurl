package de.knurl.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.knurl.KnurlApp
import de.knurl.R
import de.knurl.data.NotificationSettings
import de.knurl.data.TrainingData
import de.knurl.domain.ReminderTimes
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Volume
import de.knurl.domain.calc.Weeks
import de.knurl.feature.common.Formatting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** The local reminders: training days, the Saturday streak nudge and the Sunday review. */
enum class Reminder { TRAINING, STREAK, WEEKLY }

/**
 * Schedules each reminder as an inexact alarm (a few minutes late is fine). Each alarm, when it
 * fires, checks whether the reminder still makes sense and schedules the next one.
 */
object ReminderScheduler {

    fun schedule(context: Context, settings: NotificationSettings) {
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        Reminder.entries.forEach { reminder ->
            val next = if (enabled(reminder, settings)) nextTime(reminder, now, settings) else null
            val alarm = pending(context, reminder)
            val manager = context.getSystemService(AlarmManager::class.java)
            if (next == null) manager.cancel(alarm)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant(zone).toEpochMilliseconds(), alarm)
        }
    }

    private fun enabled(reminder: Reminder, s: NotificationSettings) = when (reminder) {
        Reminder.TRAINING -> s.reminders
        Reminder.STREAK -> s.streak
        Reminder.WEEKLY -> s.weeklyReview
    }

    private fun nextTime(reminder: Reminder, now: LocalDateTime, s: NotificationSettings): LocalDateTime? = when (reminder) {
        Reminder.TRAINING -> ReminderTimes.next(now, s.reminderDays, s.reminderMinutes)
        Reminder.STREAK -> ReminderTimes.nextStreakCheck(now)
        Reminder.WEEKLY -> ReminderTimes.nextWeeklyReview(now)
    }

    private fun pending(context: Context, reminder: Reminder) = PendingIntent.getBroadcast(
        context, 100 + reminder.ordinal,
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_REMINDER, reminder.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    const val EXTRA_REMINDER = "reminder"
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER)?.let(Reminder::valueOf) ?: return
        val app = context.applicationContext as KnurlApp
        val done = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // A cold start reads the data from Firestore's cache; give it a moment.
                val data = withTimeoutOrNull(10_000) { app.container.training.data.filterNotNull().first() }
                if (data != null) {
                    show(context, reminder, data)
                    ReminderScheduler.schedule(context, data.settings.notifications)
                }
            } finally {
                done.finish()
            }
        }
    }

    private fun show(context: Context, reminder: Reminder, data: TrainingData) {
        val zone = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Weeks.dateOf(nowMs, zone)
        val week = Weeks.inWeek(data.workouts, Weeks.startOf(today), zone)
        val goal = data.settings.weeklyGoal
        when (reminder) {
            Reminder.TRAINING -> {
                val trainedToday = data.workouts.any { Weeks.dateOf(it.startedAtMs, zone) == today }
                if (trainedToday || data.active != null) return
                Notifications.show(
                    context, Channel.REMINDERS, Notifications.ID_REMINDER,
                    context.getString(R.string.reminder_training_title),
                    context.resources.getQuantityString(R.plurals.reminder_training_body, goal, week.size, goal),
                )
            }
            Reminder.STREAK -> {
                val streakSoFar = Weeks.streak(data.workouts, today.minus(7, DateTimeUnit.DAY), zone, goal)
                if (week.size != goal - 1 || streakSoFar == 0) return
                Notifications.show(
                    context, Channel.STREAK, Notifications.ID_STREAK,
                    context.getString(R.string.reminder_streak_title),
                    context.resources.getQuantityString(R.plurals.reminder_streak_body, streakSoFar, streakSoFar),
                )
            }
            Reminder.WEEKLY -> {
                if (week.isEmpty()) return
                val ids = week.map { it.id }.toSet()
                val records = History.records(data.workouts).count { it.workoutId in ids }
                Notifications.show(
                    context, Channel.WEEKLY, Notifications.ID_WEEKLY,
                    context.getString(R.string.reminder_weekly_title),
                    context.getString(
                        R.string.reminder_weekly_body,
                        context.resources.getQuantityString(R.plurals.workouts_count, week.size, week.size),
                        Formatting.tonnage(week.sumOf(Volume::of), data.settings.unit),
                        context.resources.getQuantityString(R.plurals.records_count, records, records),
                    ),
                )
            }
        }
    }
}

/** Alarms do not survive a restart; schedule them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as KnurlApp
        val done = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val data = withTimeoutOrNull(10_000) { app.container.training.data.filterNotNull().first() }
                ReminderScheduler.schedule(context, data?.settings?.notifications ?: NotificationSettings())
            } finally {
                done.finish()
            }
        }
    }
}
