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
import de.knurl.R
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
import de.knurl.feature.common.WeeklyBars
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
            item { SectionHeader(stringResource(R.string.progress_this_week)) }
            item {
                KpiRow(
                    listOf(
                        Kpi("${s.sessions}", stringResource(R.string.stat_sessions)),
                        Kpi(tonnageText(s.volumeKg), stringResource(R.string.stat_volume)),
                        Kpi("${s.records}", stringResource(R.string.stat_records)),
                    ),
                )
            }

            item { SectionHeader(stringResource(R.string.progress_hard_sets)) }
            item {
                KnurlCard {
                    s.hardSets.forEach { (muscle, sets) -> HardSetBar(muscle, sets) }
                    Text(
                        stringResource(R.string.progress_hard_sets_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.progress_volume_weeks)) }
            item { KnurlCard { WeeklyBars(s.weeklyVolumeKg) } }

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

@Composable
private fun HardSetBar(muscle: Muscle, sets: Double) {
    val low = sets < HardSets.weeklyTarget.start
    val zone = MaterialTheme.colorScheme.primaryContainer
    val fill = if (low) Knurl.colors.warning else MaterialTheme.colorScheme.primary
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
            String.format(LocalConfiguration.current.locales[0], if (sets % 1.0 == 0.0) "%.0f" else "%.1f", sets),
            Modifier.width(32.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
        )
    }
}
