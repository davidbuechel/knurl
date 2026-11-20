package de.knurl.core.notifications

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import de.knurl.R
import de.knurl.core.workout.RestTimer
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The ongoing notification while a workout runs: name, sets done, and a live clock. The clock is
 * Android's own chronometer (elapsed time, or a countdown during rest), so nothing has to tick.
 */
class WorkoutNotifier(
    private val context: Context,
    private val training: TrainingRepository,
    private val rest: RestTimer,
    scope: CoroutineScope,
) {
    private data class Shown(val name: String, val startedAtMs: Long, val done: Int, val total: Int, val restEndsAtMs: Long?)

    init {
        scope.launch {
            combine(training.data, rest.endsAtMs) { data, restEnd ->
                val active = data?.active
                if (active == null || data.settings.notifications.workoutRunning.not()) null
                else Shown(
                    name = active.name,
                    startedAtMs = active.startedAtMs,
                    done = active.exercises.sumOf { e -> e.sets.count { it.done } },
                    total = active.exercises.sumOf { it.sets.size },
                    restEndsAtMs = restEnd?.takeIf { it > System.currentTimeMillis() },
                )
            }.distinctUntilChanged().collect(::render)
        }
    }

    @SuppressLint("MissingPermission") // checked via permissionGranted
    private fun render(shown: Shown?) {
        if (shown == null || !Notifications.permissionGranted(context)) {
            Notifications.cancel(context, Notifications.ID_WORKOUT)
            return
        }
        val resting = shown.restEndsAtMs != null
        val notification = Notifications.builder(context, Channel.WORKOUT)
            .setContentTitle(shown.name)
            .setContentText(
                if (resting) context.getString(R.string.workout_notification_rest, shown.done, shown.total)
                else context.getString(R.string.workout_notification_sets, shown.done, shown.total),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(resting)
            .setWhen(shown.restEndsAtMs ?: shown.startedAtMs)
            .setContentIntent(Notifications.tapIntent(context, Notifications.TARGET_WORKOUT, Notifications.ID_WORKOUT))
            .build()
        NotificationManagerCompat.from(context).notify(Notifications.ID_WORKOUT, notification)
    }
}
