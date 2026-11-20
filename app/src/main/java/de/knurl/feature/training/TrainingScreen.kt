package de.knurl.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.ActiveWorkoutRoute
import de.knurl.ui.navigation.Navigator

@Composable
fun TrainingScreen(go: Navigator) {
    val vm = knurlViewModel { TrainingViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val german = isGerman()
    val freeName = stringResource(R.string.workout_free_name)
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(stringResource(R.string.tab_training)) }

        item {
            FilledTonalButton(onClick = { vm.start(null, freeName); go.to(ActiveWorkoutRoute) }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(if (s.hasActive) R.string.action_resume else R.string.action_start_free))
            }
        }

        item { SectionLabel(stringResource(R.string.training_routines), Modifier.padding(top = 8.dp)) }
        items(s.routines, key = { it.id }) { routine ->
            KnurlCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            routine.items.joinToString(" · ") { vm.exerciseName(it.exerciseId, german) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalIconButton(onClick = { vm.start(routine, freeName); go.to(ActiveWorkoutRoute) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start_workout))
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.training_history), Modifier.padding(top = 8.dp)) }
        if (s.history.isEmpty()) {
            item { BrandEmptyState(stringResource(R.string.empty_history_title), stringResource(R.string.empty_history_body)) }
        }
        items(s.history, key = { it.workout.id }) { row ->
            KnurlCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(row.workout.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                R.string.history_line,
                                Formatting.date(row.workout.startedAtMs),
                                Formatting.duration(row.durationSec),
                                row.sets,
                                Formatting.tonnage(row.volumeKg),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { deleting = row.workout.id }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }

    deleting?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_workout_title),
            body = stringResource(R.string.delete_workout_body),
            confirm = stringResource(R.string.action_delete),
            onDismiss = { deleting = null },
            onConfirm = { vm.delete(id); deleting = null },
        )
    }
}
