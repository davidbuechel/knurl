package de.knurl.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.knurl.domain.calc.Change
import de.knurl.domain.calc.LoadLevel
import de.knurl.domain.calc.MuscleLoad
import de.knurl.domain.calc.Period
import de.knurl.feature.common.LabeledBars
import de.knurl.feature.common.SegmentedTabs
import de.knurl.feature.common.Sparkline
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.weightText
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.brand.BodyMap
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.loadColor
import de.knurl.ui.navigation.ExerciseRoute
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import de.knurl.R
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.estimateText
import de.knurl.ui.navigation.Navigator
import de.knurl.feature.common.tonnageText
import de.knurl.domain.calc.HardSets
import de.knurl.domain.model.Muscle
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.label
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.theme.Knurl

/** The bars run to this many sets; the target zone (10–20) sits inside. */
private const val SCALE_MAX = 25.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(go: Navigator) {
    val vm = knurlViewModel { ProgressViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedMuscle by rememberSaveable { mutableStateOf<Muscle?>(null) }
    val german = isGerman()
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SegmentedTabs(Period.entries, s.period, {
                    stringResource(when (it) { Period.WEEK -> R.string.period_week; Period.MONTH -> R.string.period_month; Period.YEAR -> R.string.period_year })
                }, vm::setPeriod)
            }

            item { SectionHeader(stringResource(R.string.progress_overview)) }
            item {
                KpiRow(
                    listOf(
                        Kpi("${s.stats.workouts}", stringResource(R.string.stat_sessions), Change(s.stats.workouts.toDouble(), s.previous.workouts.toDouble()).percent),
                        Kpi(tonnageText(s.stats.volumeKg), stringResource(R.string.stat_volume), Change(s.stats.volumeKg, s.previous.volumeKg).percent),
                        Kpi("${s.stats.records}", stringResource(R.string.stat_records)),
                    ),
                )
            }
            item {
                KpiRow(
                    listOf(
                        Kpi("${s.stats.trainingDays}", stringResource(R.string.stat_days)),
                        Kpi(hoursText(s.stats.minutes, s.stats.workouts), stringResource(R.string.stat_time)),
                        Kpi(s.kcal?.let { "≈ " + Formatting.kg(it.toDouble()) } ?: "–", stringResource(R.string.stat_kcal), s.kcal?.let { now -> s.kcalPrevious?.let { Change(now.toDouble(), it.toDouble()).percent } }),
                    ),
                )
            }

            item { SectionHeader(stringResource(R.string.progress_muscles)) }
            item {
                KnurlCard {
                    BodyMap(s.levels, selected = selectedMuscle, onSelect = { selectedMuscle = if (selectedMuscle == it) null else it })
                    val chosen = selectedMuscle
                    Text(
                        if (chosen != null) stringResource(R.string.progress_muscle_selected, stringResource(chosen.label()), setsText(s.muscleWeekly.first { it.first == chosen }.second, locale))
                        else stringResource(R.string.progress_muscles_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LoadLegend()
                }
            }
            item {
                KnurlCard {
                    s.muscleWeekly.forEach { (muscle, sets) -> HardSetBar(muscle, sets) }
                    Text(
                        stringResource(R.string.progress_hard_sets_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.progress_gains))
                Text(
                    stringResource(when (ProgressViewModel.improverDays(s.period)) { 28 -> R.string.progress_gains_28; 90 -> R.string.progress_gains_90; else -> R.string.progress_gains_365 }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
            item {
                if (s.improvers.isEmpty()) {
                    BrandEmptyState(stringResource(R.string.progress_gains), stringResource(R.string.progress_gains_empty), icon = Icons.AutoMirrored.Rounded.TrendingUp)
                } else {
                    ListGroup {
                        s.improvers.forEachIndexed { i, row ->
                            ListRow(
                                row.exercise?.name(german) ?: row.improvement.exerciseId,
                                subtitle = "${estimateText(row.improvement.fromKg)} → ${estimateText(row.improvement.toKg)}",
                                leading = { row.exercise?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                                trailing = {
                                    Sparkline(row.improvement.series.map { it.value }, Modifier.width(56.dp).height(28.dp))
                                    Text(
                                        Formatting.percent(row.improvement.percent.roundToInt(), signed = true),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Knurl.colors.success,
                                    )
                                },
                                divider = i < s.improvers.lastIndex,
                                onClick = { go.to(ExerciseRoute(row.improvement.exerciseId)) },
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(when (s.period) { Period.WEEK -> R.string.progress_volume_weeks; Period.MONTH -> R.string.progress_volume_months6; Period.YEAR -> R.string.progress_volume_months12 }))
            }
            item {
                KnurlCard {
                    LabeledBars(s.volumeBuckets, s.bucketStarts.map { bucketLabel(it, s.period, locale) })
                }
            }

            item { SectionHeader(stringResource(R.string.progress_streak)) }
            item {
                ListGroup {
                    ListRow(
                        pluralStringResource(R.plurals.progress_streak_weeks, s.streakWeeks, s.streakWeeks, s.weeklyGoal),
                        leading = { IconTile(Icons.Rounded.LocalFireDepartment, Knurl.colors.warning) },
                        divider = false,
                    )
                }
            }
        }
    }
}

/** "45 min" or "12 h": time in a period at a glance; short workouts still show as "< 1 min", not "0 min". */
private fun hoursText(minutes: Int, workouts: Int): String = when {
    minutes == 0 && workouts > 0 -> "< 1 min"
    minutes < 60 -> "$minutes min"
    else -> "${(minutes / 60.0).roundToInt()} h"
}

/** One decimal at most, none when it is whole after rounding ("11", "6.3"). */
private fun setsText(sets: Double, locale: Locale): String {
    val rounded = Math.round(sets * 10) / 10.0
    return String.format(locale, if (rounded % 1.0 == 0.0) "%.0f" else "%.1f", rounded)
}

/** Week buckets show their Monday the local way ("16.11." / "11/16"), month buckets the month ("Nov"). */
private fun bucketLabel(start: kotlinx.datetime.LocalDate, period: Period, locale: Locale): String {
    val date = java.time.LocalDate.of(start.year, start.month.ordinal + 1, start.day)
    return if (period == Period.WEEK) date.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dM"), locale))
    else date.month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, locale).take(3)
}

@Composable
private fun LoadLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(LoadLevel.NONE to R.string.load_none, LoadLevel.LOW to R.string.load_low, LoadLevel.TARGET to R.string.load_target, LoadLevel.HIGH to R.string.load_high)
            .forEach { (level, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(loadColor(level)))
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
    }
}

@Composable
private fun HardSetBar(muscle: Muscle, sets: Double) {
    // Same colours as the body map: the deeper the cobalt, the more sets.
    val zone = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val fill = loadColor(MuscleLoad.level(sets))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(muscle.label()), Modifier.width(92.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            val zoneStart = maxWidth * (HardSets.weeklyTarget.start / SCALE_MAX).toFloat()
            val zoneWidth = maxWidth * ((HardSets.weeklyTarget.endInclusive - HardSets.weeklyTarget.start) / SCALE_MAX).toFloat()
            Box(Modifier.offset(x = zoneStart).width(zoneWidth).fillMaxHeight().background(zone))
            Box(
                Modifier
                    .fillMaxWidth((sets / SCALE_MAX).coerceIn(0.0, 1.0).toFloat())
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(fill),
            )
        }
        Text(
            setsText(sets, LocalConfiguration.current.locales[0]),
            Modifier.width(40.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
        )
    }
}
