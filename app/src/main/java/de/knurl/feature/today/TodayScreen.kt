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
import de.knurl.ui.navigation.StatsRoute
import de.knurl.feature.common.weightText
import de.knurl.R
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
fun TodayScreen(go: Navigator) {
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
                        HeroButton(stringResource(R.string.routine_new)) { go.to(RoutineEditorRoute()) }
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
                listOf(
                    Kpi("${s.weekSessions}/${s.weeklyGoal}", stringResource(R.string.stat_sessions)),
                    Kpi(tonnageText(s.weekVolumeKg), stringResource(R.string.stat_volume), s.weekVolumeChange?.takeIf { s.weekVolumeKg > 0 }),
                    Kpi("${s.streakWeeks}", pluralStringResource(R.plurals.streak_weeks_label, s.streakWeeks)),
                ),
            )
        }

        if (s.gains.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.today_progress), action = stringResource(R.string.today_all), onAction = { go.to(StatsRoute) }) }
            item {
                ListGroup {
                    s.gains.forEachIndexed { i, row ->
                        ListRow(
                            title = row.exercise?.name(german) ?: row.improvement.exerciseId,
                            subtitle = "${estimateText(row.improvement.fromKg)} → ${estimateText(row.improvement.toKg)}",
                            leading = { row.exercise?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                            trailing = { Pill("+${row.improvement.percent.roundToInt()} %", color = Knurl.colors.success) },
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
private fun HeroButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
