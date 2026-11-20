package de.knurl.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.feature.common.weightText
import de.knurl.feature.common.setText
import de.knurl.feature.common.tonnageText
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.StatTile
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.ActiveWorkoutRoute
import de.knurl.ui.navigation.ExerciseRoute
import de.knurl.ui.navigation.Navigator

@Composable
fun TodayScreen(go: Navigator) {
    val vm = knurlViewModel { TodayViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val german = isGerman()
    val freeName = stringResource(R.string.workout_free_name)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenTitle(stringResource(R.string.tab_today), eyebrow = Formatting.date(System.currentTimeMillis()))
        }

        item {
            val active = s.active
            if (active != null) {
                KnurlCard(onClick = { go.to(ActiveWorkoutRoute) }) {
                    SectionLabel(stringResource(R.string.today_running))
                    Text(active.name, style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { go.to(ActiveWorkoutRoute) }) { Text(stringResource(R.string.action_resume)) }
                }
            } else {
                KnurlCard {
                    SectionLabel(stringResource(R.string.today_next))
                    val routine = s.nextRoutine
                    if (routine != null) {
                        Text(routine.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            routine.items.joinToString(" · ") { vm.exerciseName(it.exerciseId, german) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (routine != null) {
                            Button(onClick = { vm.startRoutine(routine, freeName); go.to(ActiveWorkoutRoute) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Text(stringResource(R.string.action_start_workout))
                            }
                        }
                        TextButton(onClick = { vm.startFree(freeName); go.to(ActiveWorkoutRoute) }) {
                            Text(stringResource(R.string.action_start_free))
                        }
                    }
                }
            }
        }

        item {
            KnurlCard {
                SectionLabel(stringResource(R.string.today_this_week))
                Row(Modifier.fillMaxWidth()) {
                    StatTile("${s.weekSessions}/${s.weeklyGoal}", stringResource(R.string.stat_sessions))
                    StatTile(tonnageText(s.weekVolumeKg), stringResource(R.string.stat_volume))
                    StatTile("${s.weekRecords}", stringResource(R.string.stat_records))
                }
            }
        }

        item { SectionLabel(stringResource(R.string.today_recent_records), Modifier.padding(top = 8.dp)) }
        if (s.recentRecords.isEmpty()) {
            item { BrandEmptyState(stringResource(R.string.empty_records_title), stringResource(R.string.empty_records_body)) }
        }
        items(s.recentRecords, key = { it.workoutId + it.exerciseId }) { record ->
            KnurlCard(onClick = { go.to(ExerciseRoute(record.exerciseId)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Column(Modifier.weight(1f)) {
                        Text(vm.exerciseName(record.exerciseId, german), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${setText(record.set.weightKg, record.set.reps)} · ${Formatting.date(record.atMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(stringResource(R.string.unit_e1rm, weightText(record.oneRepMaxKg)), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

