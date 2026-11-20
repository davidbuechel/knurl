package de.knurl.core.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import de.knurl.KnurlApp
import de.knurl.R
import de.knurl.core.notifications.Channel
import de.knurl.core.notifications.Notifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The rest between sets, app-wide: the workout screen shows it, the ongoing notification counts it
 * down, and an alarm says "rest is over" when the app is in the background.
 */
class RestTimer(private val context: Context, private val now: () -> Long = System::currentTimeMillis) {

    private val _endsAtMs = MutableStateFlow<Long?>(null)
    /** When the current rest ends, or null when no rest runs. */
    val endsAtMs: StateFlow<Long?> = _endsAtMs.asStateFlow()

    fun start(seconds: Int) = set(now() + seconds * 1000L)

    fun add(seconds: Int) {
        _endsAtMs.value?.let { set(it + seconds * 1000L) }
    }

    fun stop() {
        _endsAtMs.value = null
        alarms().cancel(pendingAlarm())
    }

    private fun set(endsAt: Long) {
        _endsAtMs.value = endsAt
        val alarm = pendingAlarm()
        val manager = alarms()
        // Exact when the person allowed it (Settings); otherwise Android may deliver it a little late.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, alarm)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, alarm)
        }
    }

    private fun alarms() = context.getSystemService(AlarmManager::class.java)

    private fun pendingAlarm() = PendingIntent.getBroadcast(
        context, 0, Intent(context, RestOverReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** True when the timer is set and has run out. */
    fun isOver(): Boolean = _endsAtMs.value?.let { now() >= it } ?: false

    fun clearIfOver() {
        if (isOver()) _endsAtMs.value = null
    }
}

/** Shows "rest is over", unless the app is open: then the workout screen gives the feedback itself. */
class RestOverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KnurlApp
        val settings = app.container.training.data.value?.settings?.notifications
        if (app.inForeground || settings?.restTimer == false) return
        Notifications.show(
            context, Channel.REST, Notifications.ID_REST,
            context.getString(R.string.rest_over_title), context.getString(R.string.rest_over_body),
            target = Notifications.TARGET_WORKOUT,
        )
    }
}
