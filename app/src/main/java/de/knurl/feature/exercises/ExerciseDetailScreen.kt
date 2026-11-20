package de.knurl.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.PlateBar
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.TrendChart
import de.knurl.feature.common.displayName
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.subtitle
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.theme.Knurl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(go: Navigator, exerciseId: String) {
    val vm = knurlViewModel(key = "exercise-$exerciseId") { ExerciseDetailViewModel(it.training, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.exercise?.displayName().orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(s.exercise.subtitle(), color = MaterialTheme.colorScheme.onSurfaceVariant) }

            item {
                KnurlCard {
                    SectionLabel(stringResource(R.string.detail_e1rm))
                    if (s.bestOneRepMaxKg == null) {
                        Text(stringResource(R.string.detail_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.unit_kg_value, Formatting.kg(Math.round(s.bestOneRepMaxKg * 2) / 2.0)), style = MaterialTheme.typography.displaySmall)
                        val gain = s.bestOneRepMaxKg - (s.firstOneRepMaxKg ?: s.bestOneRepMaxKg)
                        if (gain > 0) {
                            Text(
                                stringResource(R.string.detail_gain, Formatting.kg(Math.round(gain * 2) / 2.0), s.sessions.size),
                                color = Knurl.colors.success,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        TrendChart(s.sessions.mapNotNull { it.bestOneRepMax }.takeLast(12), format = { Formatting.kg(it) })
                    }
                }
            }

            if (s.bestOneRepMaxKg != null) item {
                KnurlCard {
                    SectionLabel(stringResource(R.string.detail_records))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.heaviestKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.detail_heaviest, Formatting.kg(it))) }) }
                        s.bestVolumeKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.detail_best_volume, Formatting.tonnage(it))) }) }
                    }
                }
            }

            s.plates?.let { load ->
                item {
                    KnurlCard {
                        SectionLabel(stringResource(R.string.detail_plates, Formatting.kg(s.plateTargetKg)))
                        PlateBar(load.perSide)
                        Text(
                            if (load.perSide.isEmpty()) stringResource(R.string.plates_empty_bar, Formatting.kg(s.barKg))
                            else stringResource(R.string.plates_per_side, load.perSide.joinToString(" + ") { Formatting.kg(it) }, Formatting.kg(s.barKg)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!load.isExact) {
                            Text(stringResource(R.string.plates_missing, Formatting.kg(load.missingKg)), color = Knurl.colors.warning, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(-2.5, 2.5, 10.0).forEach { delta ->
                                FilledTonalButton(onClick = { vm.changePlateTarget(delta) }) {
                                    Text((if (delta < 0) "−" else "+") + Formatting.kg(kotlin.math.abs(delta)))
                                }
                            }
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.detail_history), Modifier.padding(top = 8.dp)) }
            if (s.sessions.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_exercise_title), stringResource(R.string.empty_exercise_body)) }
            }
            items(s.sessions.reversed(), key = { it.workoutId }) { session ->
                KnurlCard {
                    Text(Formatting.date(session.atMs), style = MaterialTheme.typography.titleSmall)
                    Text(
                        session.sets.joinToString("  ·  ") { Formatting.set(it.weightKg, it.reps) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
