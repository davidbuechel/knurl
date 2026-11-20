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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.foundation.layout.Row
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
import de.knurl.feature.common.SegmentedTabs
import de.knurl.feature.common.Pill
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.Kpi
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import de.knurl.feature.common.tonnageText
import de.knurl.feature.common.weightText
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.data.WeightUnit
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.PlateBar
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.TrendChart
import de.knurl.feature.common.displayName
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.label
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.theme.Knurl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(go: Navigator, exerciseId: String) {
    val vm = knurlViewModel(key = "exercise-$exerciseId") { ExerciseDetailViewModel(it.training, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current
    val german = isGerman()
    var tab by rememberSaveable { mutableStateOf(DetailTab.HOW_TO) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.exercise?.displayName().orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    val favorite = state?.favorite == true
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = stringResource(if (favorite) R.string.favorite_remove else R.string.favorite_add),
                            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            item { ExerciseGallery(s.exercise) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(stringResource(s.exercise.primary.label()))
                    s.exercise.secondary.forEach { muscle -> Pill(stringResource(muscle.label()), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Pill(stringResource(s.exercise.equipment.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill(stringResource(s.exercise.level.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SegmentedTabs(DetailTab.entries, tab, {
                    stringResource(when (it) {
                        DetailTab.HOW_TO -> R.string.detail_how_to
                        DetailTab.HISTORY -> R.string.detail_history
                        DetailTab.RECORDS -> R.string.detail_records
                    })
                }, { tab = it })
            }

            when (tab) {
                DetailTab.HOW_TO -> {
                    val steps = s.exercise.instructions(german)
                    item {
                        if (steps.isEmpty()) {
                            BrandEmptyState(stringResource(R.string.detail_no_instructions_title), stringResource(R.string.detail_no_instructions_body))
                        } else {
                            ListGroup {
                                if (german && s.exercise.instructionsDe.isEmpty()) {
                                    Text(stringResource(R.string.detail_instructions_english), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                                }
                                steps.forEachIndexed { index, step ->
                                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                DetailTab.HISTORY -> {
                    item {
                        KnurlCard {
                            SectionLabel(stringResource(R.string.detail_e1rm))
                            if (s.bestOneRepMaxKg == null) {
                                Text(stringResource(R.string.detail_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(weightText(Math.round(s.bestOneRepMaxKg * 2) / 2.0), style = MaterialTheme.typography.displaySmall)
                                val gain = s.bestOneRepMaxKg - (s.firstOneRepMaxKg ?: s.bestOneRepMaxKg)
                                if (gain > 0) {
                                    Text(stringResource(R.string.detail_gain, weightText(Math.round(gain * 2) / 2.0), s.sessions.size), color = Knurl.colors.success, style = MaterialTheme.typography.labelLarge)
                                }
                                TrendChart(s.sessions.mapNotNull { it.bestOneRepMax }.takeLast(12), format = { Formatting.weightNumber(it, unit) })
                            }
                        }
                    }
                    item {
                        if (s.sessions.isEmpty()) {
                            BrandEmptyState(stringResource(R.string.empty_exercise_title), stringResource(R.string.empty_exercise_body))
                        } else {
                            ListGroup {
                                val sessions = s.sessions.reversed()
                                sessions.forEachIndexed { i, session ->
                                    ListRow(
                                        title = Formatting.date(session.atMs),
                                        subtitle = session.sets.joinToString("  ·  ") { Formatting.set(it.weightKg, it.reps, unit) },
                                        divider = i < sessions.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                }
                DetailTab.RECORDS -> {
                    item {
                        KpiRow(
                            listOf(
                                Kpi(s.bestOneRepMaxKg?.let { weightText(Math.round(it * 2) / 2.0) } ?: "–", stringResource(R.string.detail_e1rm)),
                                Kpi(s.heaviestKg?.let { weightText(it) } ?: "–", stringResource(R.string.detail_heaviest_label)),
                                Kpi(s.bestVolumeKg?.let { tonnageText(it) } ?: "–", stringResource(R.string.detail_best_volume_label)),
                            ),
                        )
                    }
                    s.plates?.let { load ->
                        item {
                            KnurlCard {
                                SectionLabel(stringResource(R.string.detail_plates, "${Formatting.kg(s.plateTargetInUnit)} ${Formatting.symbol(unit)}"))
                                PlateBar(load.perSide, unit)
                                Text(
                                    if (load.perSide.isEmpty()) stringResource(R.string.plates_empty_bar, "${Formatting.kg(s.barInUnit)} ${Formatting.symbol(unit)}")
                                    else stringResource(R.string.plates_per_side, load.perSide.joinToString(" + ") { Formatting.kg(it) }, "${Formatting.kg(s.barInUnit)} ${Formatting.symbol(unit)}"),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (!load.isExact) {
                                    Text(stringResource(R.string.plates_missing, "${Formatting.kg(load.missingKg)} ${Formatting.symbol(unit)}"), color = Knurl.colors.warning, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val steps = if (unit == WeightUnit.LB) listOf(-5.0, 5.0, 20.0) else listOf(-2.5, 2.5, 10.0)
                                    steps.forEach { delta ->
                                        FilledTonalButton(onClick = { vm.changePlateTarget(Formatting.fromUnit(delta, unit)) }) {
                                            Text((if (delta < 0) "−" else "+") + Formatting.kg(kotlin.math.abs(delta)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DetailTab { HOW_TO, HISTORY, RECORDS }
