package de.knurl.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.knurl.MainActivity
import de.knurl.R
import de.knurl.data.NotificationSettings

/**
 * One Android channel per kind of notification, so people can also switch them off in the system
 * settings. The ids are the `channel` values push messages carry (docs/notifications.md).
 */
enum class Channel(val id: String, val title: Int, val importance: Int) {
    REST("rest", R.string.notify_rest, NotificationManager.IMPORTANCE_HIGH),
    // Default importance but without sound or vibration: a silent (low) channel is hidden from the
    // lock screen on many phones, and the workout is meant to be used from there.
    WORKOUT("workout_live", R.string.notify_running, NotificationManager.IMPORTANCE_DEFAULT),
    REMINDERS("reminders", R.string.notify_reminders, NotificationManager.IMPORTANCE_DEFAULT),
    STREAK("streak", R.string.notify_streak, NotificationManager.IMPORTANCE_DEFAULT),
    WEEKLY("weekly", R.string.notify_weekly, NotificationManager.IMPORTANCE_DEFAULT),
    ACCOUNT("account", R.string.notify_account, NotificationManager.IMPORTANCE_DEFAULT),
    NEWS("news", R.string.notify_news, NotificationManager.IMPORTANCE_LOW);

    /** Whether the person wants this kind (Settings → Notifications). */
    fun enabledIn(settings: NotificationSettings): Boolean = when (this) {
        REST -> settings.restTimer
        WORKOUT -> settings.workoutRunning
        REMINDERS -> settings.reminders
        STREAK -> settings.streak
        WEEKLY -> settings.weeklyReview
        ACCOUNT -> settings.account
        NEWS -> settings.news
    }

    companion object {
        fun byId(id: String?): Channel? = entries.firstOrNull { it.id == id }
    }
}

object Notifications {
    /** Fixed ids: one notification of each kind at a time. */
    const val ID_WORKOUT = 1
    const val ID_REST = 2
    const val ID_REMINDER = 3
    const val ID_STREAK = 4
    const val ID_WEEKLY = 5

    /** Where a tap leads (MainActivity reads it). */
    const val EXTRA_TARGET = "target"
    const val TARGET_WORKOUT = "workout"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            Channel.entries.map { channel ->
                NotificationChannel(channel.id, context.getString(channel.title), channel.importance).apply {
                    if (channel == Channel.WORKOUT) {
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                }
            },
        )
        // Replaced by "workout_live" (a channel's importance can't be raised once created).
        manager.deleteNotificationChannel("workout")
    }

    /** Android 13+ needs a runtime permission; older versions always allow. */
    fun permissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun tapIntent(context: Context, target: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (target != null) putExtra(EXTRA_TARGET, target)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun builder(context: Context, channel: Channel): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_cobalt))

    fun show(context: Context, channel: Channel, id: Int, title: String, body: String, target: String? = null) {
        if (!permissionGranted(context)) return
        val notification = builder(context, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(tapIntent(context, target, id))
            .build()
        @Suppress("MissingPermission") // checked above
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)
}
