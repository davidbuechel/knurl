package de.knurl.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import de.knurl.R
import de.knurl.ui.navigation.AchievementsRoute
import de.knurl.ui.theme.Knurl
import androidx.compose.material.icons.rounded.MilitaryTech
import de.knurl.domain.achievements.Achievements
import de.knurl.feature.achievements.achievements
import de.knurl.data.firebase.AuthRepository
import de.knurl.data.firebase.AuthState
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.Volume
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.tonnageText
import de.knurl.feature.common.weightText
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.AccountRoute
import de.knurl.ui.navigation.BodyWeightRoute
import de.knurl.ui.navigation.CalendarRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.SettingsRoute
import de.knurl.ui.navigation.StatsRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class ProfileState(
    val anonymous: Boolean,
    val email: String?,
    val workouts: Int,
    val totalVolumeKg: Double,
    val records: Int,
    val firstWorkoutMs: Long?,
    val latestWeightKg: Double?,
    val trainingDaysThisMonth: Int,
    val achievementsUnlocked: Int,
    val achievementsTotal: Int,
)

class ProfileViewModel(training: TrainingRepository, private val auth: AuthRepository) : ViewModel() {
    val state: StateFlow<ProfileState?> = combine(training.data.filterNotNull(), auth.state) { data, authState ->
        val zone = ZoneId.systemDefault()
        val month = YearMonth.now()
        ProfileState(
            anonymous = (authState as? AuthState.SignedIn)?.anonymous ?: true,
            email = auth.email(),
            workouts = data.workouts.size,
            totalVolumeKg = data.workouts.sumOf(Volume::of),
            records = History.records(data.workouts).size,
            firstWorkoutMs = data.workouts.minOfOrNull { it.startedAtMs },
            latestWeightKg = data.bodyWeights.lastOrNull()?.weightKg,
            achievementsUnlocked = data.achievements().count { it.unlocked },
            achievementsTotal = Achievements.all.size,
            trainingDaysThisMonth = data.workouts.map { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate() }
                .filter { YearMonth.from(it) == month }.toSet().size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Profile: who you are, what you've done, and everything else in clear groups. */
@Composable
fun ProfileScreen(go: Navigator) {
    val vm = knurlViewModel { ProfileViewModel(it.training, it.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val shortName = s.email?.takeIf { it.isNotBlank() }?.substringBefore('@')?.replaceFirstChar { it.uppercase() }
    val name = shortName ?: stringResource(R.string.profile_you)

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                Box(Modifier.size(64.dp).clip(CircleShape).background(brush), contentAlignment = Alignment.Center) {
                    // An initial only for a real name; otherwise a neutral person.
                    if (shortName != null) Text(shortName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(
                            s.firstWorkoutMs?.let { stringResource(R.string.profile_since_value, Formatting.monthYear(it)) },
                            pluralStringResource(R.plurals.workouts_count, s.workouts, s.workouts),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (s.anonymous) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { go.to(AccountRoute) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.account_anonymous), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(R.string.profile_secure_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    Text(stringResource(R.string.account_secure_cta), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            KpiRow(
                listOf(
                    Kpi("${s.workouts}", stringResource(R.string.profile_workouts)),
                    Kpi(tonnageText(s.totalVolumeKg), stringResource(R.string.profile_total_volume)),
                    Kpi("${s.records}", stringResource(R.string.stat_records)),
                ),
            )
        }

        item { SectionHeader(stringResource(R.string.profile_progress)) }
        item {
            ListGroup {
                ListRow(stringResource(R.string.stats_title), subtitle = stringResource(R.string.stats_subtitle_short), leading = { IconTile(Icons.Rounded.Insights) }) { go.to(StatsRoute) }
                ListRow(
                    stringResource(R.string.achievements_title),
                    subtitle = stringResource(R.string.achievements_hint),
                    leading = { IconTile(Icons.Rounded.MilitaryTech, Knurl.colors.gold) },
                    trailing = {
                        Text("${s.achievementsUnlocked}/${s.achievementsTotal}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    onClick = { go.to(AchievementsRoute) },
                )
                ListRow(
                    stringResource(R.string.body_weight),
                    leading = { IconTile(Icons.Rounded.MonitorWeight, MaterialTheme.colorScheme.secondary) },
                    trailing = {
                        s.latestWeightKg?.let { Text(weightText(it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    onClick = { go.to(BodyWeightRoute) },
                )
                ListRow(
                    stringResource(R.string.calendar),
                    leading = { IconTile(Icons.Rounded.CalendarMonth, MaterialTheme.colorScheme.secondary) },
                    trailing = {
                        Text(pluralStringResource(R.plurals.calendar_days, s.trainingDaysThisMonth, s.trainingDaysThisMonth), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    divider = false,
                    onClick = { go.to(CalendarRoute) },
                )
            }
        }

        item { SectionHeader(stringResource(R.string.profile_app)) }
        item {
            ListGroup {
                ListRow(stringResource(R.string.settings_title), subtitle = stringResource(R.string.settings_subtitle), leading = { IconTile(Icons.Rounded.Settings, MaterialTheme.colorScheme.outline) }) { go.to(SettingsRoute) }
                ListRow(stringResource(R.string.profile_account_data), subtitle = stringResource(R.string.profile_account_data_hint), leading = { IconTile(Icons.Rounded.ManageAccounts, MaterialTheme.colorScheme.outline) }, divider = false) { go.to(AccountRoute) }
            }
        }
    }
}
