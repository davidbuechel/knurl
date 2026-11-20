package de.knurl.core.workout

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.knurl.KnurlApp
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.SetRef
import de.knurl.domain.WorkoutFlow
import de.knurl.domain.updateSet
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What can be done to the running workout without opening the app (the buttons of the workout
 * notification, also on the lock screen). Same rules as ticking a set in the workout screen.
 */
class WorkoutControl(private val training: TrainingRepository, private val rest: RestTimer) {

    /**
     * Ticks [ref] off, if it is still the open set the notification showed (a stale button never
     * ticks a different set), and starts the rest the way the workout screen would.
     */
    fun complete(ref: SetRef): Boolean {
        val workout = training.data.value?.active ?: return false
        val item = workout.exercises.getOrNull(ref.exercise) ?: return false
        val set = item.sets.getOrNull(ref.set) ?: return false
        if (set.done) return false
        training.updateActive { it.updateSet(ref.exercise, ref.set) { s -> s.copy(done = true) } }
        if (WorkoutFlow.restsAfter(workout, ref)) rest.start(item.restSec) else rest.stop()
        return true
    }

    fun addRest(seconds: Int) = rest.add(seconds)

    fun skipRest() = rest.stop()
}

/**
 * Runs a notification button. The app may have been closed by Android in the meantime: then this
 * waits (briefly) for the running workout to come back from the local cache before acting; the
 * change is written through Firestore's offline queue, so it is not lost if the process ends.
 */
class WorkoutActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as KnurlApp).container
        val control = WorkoutControl(container.training, container.restTimer)
        val pending = goAsync()
        container.scope.launch {
            try {
                withTimeoutOrNull(LOAD_TIMEOUT_MS) { container.training.data.filterNotNull().first() }
                when (intent.action) {
                    ACTION_DONE -> control.complete(SetRef(intent.getIntExtra(EXTRA_EXERCISE, -1), intent.getIntExtra(EXTRA_SET, -1)))
                    ACTION_REST_ADD -> control.addRest(REST_STEP_SEC)
                    ACTION_REST_SKIP -> control.skipRest()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "de.knurl.workout.DONE"
        const val ACTION_REST_ADD = "de.knurl.workout.REST_ADD"
        const val ACTION_REST_SKIP = "de.knurl.workout.REST_SKIP"
        private const val EXTRA_EXERCISE = "exercise"
        private const val EXTRA_SET = "set"
        const val REST_STEP_SEC = 15
        private const val LOAD_TIMEOUT_MS = 8_000L

        /** A button's intent. The set is part of it, so a tap acts on the set that was on screen. */
        fun intent(context: Context, action: String, ref: SetRef? = null): PendingIntent {
            val intent = Intent(context, WorkoutActionReceiver::class.java).setAction(action)
            ref?.let { intent.putExtra(EXTRA_EXERCISE, it.exercise).putExtra(EXTRA_SET, it.set) }
            val code = action.hashCode() + (ref?.let { it.exercise * 1000 + it.set } ?: 0)
            return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
