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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.Edit
import de.knurl.ui.navigation.CustomExerciseRoute
import kotlin.math.roundToInt
import de.knurl.domain.calc.Progress
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.setText
import de.knurl.R
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import de.knurl.feature.common.estimateText
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
/** With [pickForWorkout] (opened from the picker's photo), a button adds the exercise to the picker's selection. */
fun ExerciseDetailScreen(go: Navigator, exerciseId: String, pickForWorkout: Boolean = false) {
    val vm = knurlViewModel(key = "exercise-$exerciseId") { ExerciseDetailViewModel(it.training, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current
    val german = isGerman()
    var tab by rememberSaveable { mutableStateOf(DetailTab.HOW_TO) }
    var range by rememberSaveable { mutableStateOf(ChartRange.M6) }

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
                    if (state?.exercise?.isCustom == true) {
                        IconButton(onClick = { go.to(CustomExerciseRoute(id = exerciseId)) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
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
        bottomBar = {
            if (pickForWorkout) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = { go.backWithResult(Navigator.RESULT_EXERCISE, exerciseId) },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(R.string.picker_select))
                    }
                }
            }
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
                    if (!s.exercise.isCustom) Pill(stringResource(s.exercise.level.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        if (s.exercise.isCustom) {
                            ListGroup {
                                ListRow(
                                    stringResource(R.string.custom_note),
                                    subtitle = s.exercise.note.ifBlank { stringResource(R.string.custom_note_hint) },
                                    divider = false,
                                    onClick = { go.to(CustomExerciseRoute(id = exerciseId)) },
                                )
                            }
                        } else if (steps.isEmpty()) {
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
                            if (s.series.isEmpty()) {
                                Text(stringResource(R.string.detail_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                SegmentedTabs(ChartRange.entries, range, { stringResource(it.label) }, { range = it })
                                val since = range.days?.let { System.currentTimeMillis() - it * Progress.DAY_MS }
                                val shown = Progress.window(s.series, since).ifEmpty { s.series.takeLast(1) }
                                Text(estimateText(shown.maxOf { it.value }), style = MaterialTheme.typography.displaySmall)
                                Progress.change(s.exercise.id, s.series, since)?.takeIf { it.deltaKg > 0 }?.let { change ->
                                    Text(
                                        stringResource(
                                            R.string.detail_change,
                                            "+" + estimateText(change.deltaKg),
                                            Formatting.percent(change.percent.roundToInt(), signed = true),
                                        ),
                                        color = Knurl.colors.success,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                TrendChart(shown.map { Formatting.toUnit(it.value, unit) }, format = { Formatting.kg(it) })
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
                                Kpi(s.bestOneRepMaxKg?.let { estimateText(it) } ?: "–", stringResource(R.string.detail_e1rm)),
                                Kpi(s.heaviestKg?.let { weightText(it) } ?: "–", stringResource(R.string.detail_heaviest_label)),
                                Kpi(s.bestVolumeKg?.let { tonnageText(it) } ?: "–", stringResource(R.string.detail_best_volume_label)),
                            ),
                        )
                    }
                    if (s.repMaxes.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.detail_rep_maxes)) }
                        item {
                            ListGroup {
                                // Rep counts with the same best set collapse into one row ("1–5 reps").
                                val rows = s.repMaxes.entries.groupBy { it.value }.map { (set, entries) -> Triple(entries.first().key, entries.last().key, set) }
                                rows.forEachIndexed { i, (from, to, set) ->
                                    ListRow(
                                        if (from == to) pluralStringResource(R.plurals.reps_count, from, from) else stringResource(R.string.reps_range, from, to),
                                        subtitle = if (set.reps > to) setText(set.weightKg, set.reps) else null,
                                        trailing = { Text(weightText(set.weightKg), style = MaterialTheme.typography.titleSmall) },
                                        divider = i < rows.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                    s.plates?.let { load ->
                        item {
                            KnurlCard {
                                SectionLabel(stringResource(R.string.detail_plates, "${Formatting.kg(s.plateTargetInUnit)}${Formatting.NBSP}${Formatting.symbol(unit)}"))
                                PlateBar(load.perSide, unit)
                                Text(
                                    if (load.perSide.isEmpty()) stringResource(R.string.plates_empty_bar, "${Formatting.kg(s.barInUnit)}${Formatting.NBSP}${Formatting.symbol(unit)}")
                                    else stringResource(R.string.plates_per_side, load.perSide.joinToString(" + ") { Formatting.kg(it) }, "${Formatting.kg(s.barInUnit)}${Formatting.NBSP}${Formatting.symbol(unit)}"),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (!load.isExact) {
                                    Text(stringResource(R.string.plates_missing, "${Formatting.kg(load.missingKg)}${Formatting.NBSP}${Formatting.symbol(unit)}"), color = Knurl.colors.warning, style = MaterialTheme.typography.bodySmall)
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

/** The time span the 1RM chart shows. */
private enum class ChartRange(val days: Int?, val label: Int) {
    M3(91, R.string.range_3m), M6(182, R.string.range_6m), Y1(365, R.string.range_1y), ALL(null, R.string.range_all),
}
