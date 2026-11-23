package de.knurl.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import de.knurl.ui.theme.Knurl
import de.knurl.feature.home.HomeTab
import de.knurl.feature.common.weightText
import de.knurl.R
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import de.knurl.ui.navigation.RecapRoute
import de.knurl.feature.common.IconTile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import de.knurl.feature.common.estimateText
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.HeroCard
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.Pill
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.WeekDots
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.setText
import de.knurl.feature.common.tonnageText
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.ActiveWorkoutRoute
import de.knurl.ui.navigation.ExerciseRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.RoutineEditorRoute
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Today: one clear next step, the week at a glance, and what improved lately. */
@Composable
fun TodayScreen(go: Navigator, openTab: (HomeTab) -> Unit = {}) {
    val vm = knurlViewModel { TodayViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val german = isGerman()
    val locale = LocalConfiguration.current.locales[0]
    val freeName = stringResource(R.string.workout_free_name)

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.padding(top = 8.dp, start = 4.dp)) {
                Text(Formatting.date(System.currentTimeMillis()).uppercase(locale), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(
                        when (s.hour) {
                            in 5..10 -> R.string.greeting_morning
                            in 11..17 -> R.string.greeting_day
                            else -> R.string.greeting_evening
                        },
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }

        s.recap?.let { (year, month) ->
            item {
                val name = java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
                ListGroup {
                    ListRow(
                        stringResource(R.string.recap_ready_title, name),
                        subtitle = stringResource(R.string.recap_ready_body),
                        leading = { IconTile(Icons.Rounded.AutoAwesome, Knurl.colors.gold) },
                        divider = false,
                        onClick = { go.to(RecapRoute(year, month)) },
                    )
                }
            }
        }

        item {
            val active = s.active
            val next = s.next
            HeroCard {
                val onHero = MaterialTheme.colorScheme.onPrimary
                when {
                    active != null -> {
                        Text(stringResource(R.string.today_running).uppercase(locale), style = MaterialTheme.typography.labelMedium, color = onHero.copy(alpha = 0.8f))
                        Text(active.name, style = MaterialTheme.typography.headlineMedium, color = onHero)
                        HeroButton(stringResource(R.string.action_resume)) { go.to(ActiveWorkoutRoute) }
                    }
                    next != null -> {
                        Text(stringResource(R.string.today_next_of, next.position, next.of).uppercase(locale), style = MaterialTheme.typography.labelMedium, color = onHero.copy(alpha = 0.8f))
                        Text(next.routine.name, style = MaterialTheme.typography.headlineMedium, color = onHero)
                        Text(
                            listOfNotNull(
                                pluralStringResource(R.plurals.exercises_count, next.routine.items.size, next.routine.items.size),
                                stringResource(R.string.minutes_about, next.minutes),
                                next.daysAgo?.let { if (it == 0) stringResource(R.string.last_today) else pluralStringResource(R.plurals.last_days_ago, it, it) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHero.copy(alpha = 0.85f),
                        )
                        HeroButton(stringResource(R.string.action_start_workout)) { vm.startRoutine(next.routine, freeName); go.to(ActiveWorkoutRoute) }
                    }
                    else -> {
                        Text(stringResource(R.string.today_no_routine), style = MaterialTheme.typography.headlineSmall, color = onHero)
                        HeroButton(stringResource(R.string.routine_new), Icons.Rounded.Add) { go.to(RoutineEditorRoute()) }
                    }
                }
            }
            if (active == null) {
                TextButton(onClick = { vm.startFree(freeName); go.to(ActiveWorkoutRoute) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_start_empty))
                }
            }
        }

        // One concrete thing for today instead of numbers: where the last session says "go heavier".
        val ready = s.next?.readyToGoUp.orEmpty()
        if (s.active == null && ready.isNotEmpty()) {
            item {
                ListGroup {
                    ListRow(
                        pluralStringResource(R.plurals.today_go_up_title, ready.size, ready.size),
                        subtitle = ready.take(3).joinToString(", ") { it.name(german) } + if (ready.size > 3) " +${ready.size - 3}" else "",
                        leading = { IconTile(Icons.AutoMirrored.Rounded.TrendingUp, Knurl.colors.success) },
                        divider = false,
                    )
                }
            }
        }

        // Before the first workout: how training works here, not a wall of zeros.
        if (s.firstTime) {
            item { SectionHeader(stringResource(R.string.first_title)) }
            item {
                ListGroup {
                    ListRow(stringResource(R.string.first_focus), subtitle = stringResource(R.string.first_focus_body), leading = { IconTile(Icons.Rounded.PlayArrow) })
                    ListRow(stringResource(R.string.first_weights), subtitle = stringResource(R.string.first_weights_body), leading = { IconTile(Icons.Rounded.Tune) })
                    ListRow(stringResource(R.string.first_lock), subtitle = stringResource(R.string.first_lock_body), leading = { IconTile(Icons.Rounded.Lock) }, divider = false)
                }
            }
            return@LazyColumn
        }

        item {
            SectionHeader(stringResource(R.string.today_this_week))
        }
        item {
            WeekDots(
                labels = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW_STANDALONE, locale) },
                trained = s.weekTrained,
                todayIndex = s.todayIndex,
            )
        }
        item {
            KpiRow(
                leading = { GoalRingTile(s.weekSessions, s.weeklyGoal) },
                items = listOf(
                    Kpi(tonnageText(s.weekVolumeKg), stringResource(R.string.stat_volume), s.weekVolumeChange?.takeIf { s.weekVolumeKg > 0 }),
                    Kpi("${s.streakWeeks}", pluralStringResource(R.plurals.streak_weeks_label, s.streakWeeks)),
                ),
            )
        }

        if (s.gains.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.today_progress), action = stringResource(R.string.today_all), onAction = { openTab(HomeTab.PROGRESS) }) }
            item {
                ListGroup {
                    s.gains.forEachIndexed { i, row ->
                        ListRow(
                            title = row.exercise?.name(german) ?: row.improvement.exerciseId,
                            subtitle = "${estimateText(row.improvement.fromKg)} → ${estimateText(row.improvement.toKg)}",
                            leading = { row.exercise?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                            trailing = { Pill(Formatting.percent(row.improvement.percent.roundToInt(), signed = true), color = Knurl.colors.success) },
                            divider = i < s.gains.lastIndex,
                            onClick = { go.to(ExerciseRoute(row.improvement.exerciseId)) },
                        )
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.today_recent_records)) }
        item {
            if (s.recentRecords.isEmpty()) {
                BrandEmptyState(stringResource(R.string.empty_records_title), stringResource(R.string.empty_records_body))
            } else {
                ListGroup {
                    s.recentRecords.forEachIndexed { i, row ->
                        ListRow(
                            title = row.exercise?.name(german) ?: row.record.exerciseId,
                            subtitle = "${setText(row.record.set.weightKg, row.record.set.reps)} · ${Formatting.date(row.record.atMs)}",
                            leading = { row.exercise?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                            trailing = { Pill(stringResource(R.string.record_short), color = MaterialTheme.colorScheme.tertiary) },
                            divider = i < s.recentRecords.lastIndex,
                            onClick = { go.to(ExerciseRoute(row.record.exerciseId)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroButton(text: String, icon: ImageVector = Icons.Rounded.PlayArrow, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The weekly goal as a ring: workouts done against the goal, green once it is met. Sits where the
 * first number of the row would be.
 */
@Composable
private fun RowScope.GoalRingTile(done: Int, goal: Int) {
    val progress by animateFloatAsState((done / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), label = "goal")
    val met = done >= goal
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill = if (met) Knurl.colors.success else MaterialTheme.colorScheme.primary
    Column(
        Modifier.weight(1f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.5.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                drawArc(fill, -90f, 360f * progress, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("$done/$goal", style = MaterialTheme.typography.labelMedium)
        }
        Text(stringResource(R.string.today_goal), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
