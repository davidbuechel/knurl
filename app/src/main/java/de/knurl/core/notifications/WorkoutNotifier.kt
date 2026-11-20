package de.knurl.core.notifications

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.knurl.R
import de.knurl.core.workout.RestTimer
import de.knurl.core.workout.WorkoutActionReceiver
import de.knurl.data.WeightUnit
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.SetRef
import de.knurl.domain.WorkoutFlow
import de.knurl.domain.model.SetKind
import de.knurl.domain.model.Workout
import de.knurl.feature.common.Formatting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The ongoing notification while a workout runs, made to be used from the lock screen:
 * - between sets it names the next one ("Bench Press · Set 2 of 3", "80 kg × 8") with "Set done";
 * - during rest it counts down with "+15 s" and "Skip rest".
 * On Android 16+ it asks to be a Live Update: a chip in the status bar with the clock, and a
 * progress bar with one segment per exercise on the lock screen. The clock is Android's own
 * chronometer (elapsed time, or the countdown during rest), so nothing has to tick.
 */
class WorkoutNotifier(
    private val context: Context,
    private val training: TrainingRepository,
    private val rest: RestTimer,
    scope: CoroutineScope,
) {
    /** The set the notification offers to tick off. */
    private data class NextSet(
        val ref: SetRef,
        val exercise: String,
        /** Working-set number, or null for a warm-up. */
        val number: Int?,
        val workingSets: Int,
        val weightKg: Double,
        val reps: Int,
    )

    private data class Shown(
        val name: String,
        val startedAtMs: Long,
        val done: Int,
        val total: Int,
        val restEndsAtMs: Long?,
        val next: NextSet?,
        /** Sets per exercise, for the progress segments. */
        val setsPerExercise: List<Int>,
        val unit: WeightUnit,
    )

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
                    next = nextSet(active),
                    setsPerExercise = active.exercises.map { it.sets.size }.filter { it > 0 },
                    unit = data.settings.unit,
                )
            }.distinctUntilChanged().collect(::render)
        }
    }

    private fun nextSet(workout: Workout): NextSet? {
        val ref = WorkoutFlow.upNext(workout) ?: return null
        val item = workout.exercises[ref.exercise]
        val set = item.sets[ref.set]
        val german = Locale.getDefault().language == "de"
        return NextSet(
            ref = ref,
            exercise = training.exercise(item.exerciseId)?.name(german) ?: item.exerciseId,
            number = if (set.kind == SetKind.WARMUP) null else item.sets.take(ref.set + 1).count { it.kind != SetKind.WARMUP },
            workingSets = item.sets.count { it.kind != SetKind.WARMUP },
            weightKg = set.weightKg,
            reps = set.reps,
        )
    }

    @SuppressLint("MissingPermission") // checked via permissionGranted
    private fun render(shown: Shown?) {
        if (shown == null || !Notifications.permissionGranted(context)) {
            Notifications.cancel(context, Notifications.ID_WORKOUT)
            return
        }
        val resting = shown.restEndsAtMs != null
        val next = shown.next
        val progressLine = context.resources.getQuantityString(R.plurals.workout_notification_sets, shown.total, shown.done, shown.total)
        // "80 kg × 8"; bodyweight sets just "12 reps".
        val setLine = next?.let {
            if (it.weightKg > 0) "${Formatting.weight(it.weightKg, shown.unit)} × ${it.reps}" else "${it.reps} ${context.getString(R.string.notif_reps)}"
        }
        val nextTitle = next?.let {
            if (it.number == null) context.getString(R.string.notif_next_warmup, it.exercise)
            else context.getString(R.string.notif_next_set, it.exercise, it.number, it.workingSets)
        }

        val builder = Notifications.builder(context, Channel.WORKOUT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(resting)
            .setWhen(shown.restEndsAtMs ?: shown.startedAtMs)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            // Nothing private in "Bench Press · 80 kg × 8": shown in full on the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(Notifications.tapIntent(context, Notifications.TARGET_WORKOUT, Notifications.ID_WORKOUT))
            .setRequestPromotedOngoing(true)

        when {
            resting -> {
                builder.setContentTitle(context.getString(R.string.notif_rest_title))
                    .setContentText(if (nextTitle != null) context.getString(R.string.notif_rest_next, nextTitle, setLine) else progressLine)
                    .addAction(
                        R.drawable.ic_action_add, context.getString(R.string.notif_action_rest_add, WorkoutActionReceiver.REST_STEP_SEC),
                        WorkoutActionReceiver.intent(context, WorkoutActionReceiver.ACTION_REST_ADD),
                    )
                    .addAction(
                        R.drawable.ic_action_skip, context.getString(R.string.notif_action_rest_skip),
                        WorkoutActionReceiver.intent(context, WorkoutActionReceiver.ACTION_REST_SKIP),
                    )
            }
            next != null -> {
                builder.setContentTitle(nextTitle)
                    .setContentText("$setLine · $progressLine")
                    .addAction(
                        R.drawable.ic_action_check, context.getString(R.string.notif_action_done),
                        WorkoutActionReceiver.intent(context, WorkoutActionReceiver.ACTION_DONE, next.ref),
                    )
            }
            else -> builder.setContentTitle(shown.name).setContentText(context.getString(R.string.notif_all_done))
        }
        builder.setStyle(progressStyle(shown))
        NotificationManagerCompat.from(context).notify(Notifications.ID_WORKOUT, builder.build())
    }

    /** One segment per exercise, filled by the sets done; on older Android a plain progress bar. */
    private fun progressStyle(shown: Shown): NotificationCompat.ProgressStyle {
        val accent = ContextCompat.getColor(context, R.color.brand_cobalt)
        return NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(shown.setsPerExercise.map { NotificationCompat.ProgressStyle.Segment(it).setColor(accent) })
            .setProgress(shown.done)
    }
}
