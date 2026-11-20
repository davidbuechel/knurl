package de.knurl.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.core.health.HealthStatus
import de.knurl.core.health.HealthSync
import de.knurl.data.Settings
import de.knurl.data.Sex
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.SegmentedRow
import de.knurl.feature.common.SwitchRow
import de.knurl.feature.common.ValueRow
import de.knurl.feature.common.appContainer
import java.time.Year

/**
 * Settings → Connections: Health Connect with one switch per kind of data. Switching one on asks
 * Health Connect for exactly the permissions it needs; it only stays on if they are granted.
 */
@Composable
fun HealthSettings(s: Settings, vm: SettingsViewModel) {
    val container = appContainer()
    val health = container.health
    val prefs = container.preferences
    val context = LocalContext.current
    val status by health.statusFlow.collectAsStateWithLifecycle()
    val granted by health.granted.collectAsStateWithLifecycle()
    val workouts by prefs.healthWorkouts.collectAsStateWithLifecycle()
    val heartRate by prefs.healthHeartRate.collectAsStateWithLifecycle()
    val weight by prefs.healthWeight.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<Pair<Set<String>, (Boolean) -> Unit>?>(null) }
    var editingYear by rememberSaveable { mutableStateOf(false) }

    val request = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { result ->
        pending?.let { (needed, set) -> set(result.containsAll(needed)) }
        pending = null
        health.refresh()
    }
    fun toggle(on: Boolean, needed: Set<String>, set: (Boolean) -> Unit) {
        when {
            !on -> set(false)
            granted.containsAll(needed) -> set(true)
            else -> { pending = needed to set; request.launch(needed) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(stringResource(R.string.settings_section_connections))
        ListGroup {
            when (status) {
                HealthStatus.UNAVAILABLE -> ListRow(
                    stringResource(R.string.health_title), subtitle = stringResource(R.string.health_unavailable),
                    leading = { IconTile(Icons.Outlined.MonitorHeart, MaterialTheme.colorScheme.outline) }, divider = false,
                )
                HealthStatus.NEEDS_INSTALL -> ListRow(
                    stringResource(R.string.health_title), subtitle = stringResource(R.string.health_install),
                    leading = { IconTile(Icons.Outlined.MonitorHeart) }, divider = false,
                ) { runCatching { context.startActivity(health.installIntent()) } }
                HealthStatus.AVAILABLE -> {
                    SwitchRow(
                        stringResource(R.string.health_workouts), workouts && granted.containsAll(HealthSync.WORKOUT_PERMISSIONS),
                        { on -> toggle(on, HealthSync.WORKOUT_PERMISSIONS, prefs::setHealthWorkouts) }, stringResource(R.string.health_workouts_hint),
                    )
                    SwitchRow(
                        stringResource(R.string.health_heart_rate), heartRate && granted.containsAll(HealthSync.HEART_RATE_PERMISSIONS),
                        { on -> toggle(on, HealthSync.HEART_RATE_PERMISSIONS, prefs::setHealthHeartRate) }, stringResource(R.string.health_heart_rate_hint),
                    )
                    SwitchRow(
                        stringResource(R.string.health_weight), weight && granted.containsAll(HealthSync.WEIGHT_PERMISSIONS),
                        { on -> toggle(on, HealthSync.WEIGHT_PERMISSIONS, prefs::setHealthWeight) }, stringResource(R.string.health_weight_hint),
                    )
                    ListRow(stringResource(R.string.health_manage), leading = { IconTile(Icons.Outlined.Tune, MaterialTheme.colorScheme.outline) }, divider = false) {
                        runCatching { context.startActivity(health.settingsIntent()) }
                    }
                }
            }
        }
        // Age and sex make energy from heart rate much better; both optional, only shown when it matters.
        if (status == HealthStatus.AVAILABLE && heartRate) {
            ListGroup {
                ValueRow(stringResource(R.string.profile_birth_year), s.birthYear?.toString() ?: "–", onClick = { editingYear = true })
                SegmentedRow(stringResource(R.string.profile_sex), listOf(null, Sex.FEMALE, Sex.MALE), s.sex, {
                    stringResource(when (it) { null -> R.string.profile_sex_none; Sex.FEMALE -> R.string.profile_sex_female; Sex.MALE -> R.string.profile_sex_male })
                }, { sex -> vm.update { it.copy(sex = sex) } }, divider = false)
            }
            Text(
                stringResource(R.string.profile_body_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (editingYear) {
        val thisYear = Year.now().value
        NumberInputDialog(
            stringResource(R.string.profile_birth_year), s.birthYear?.toString() ?: "", decimal = false,
            onDismiss = { editingYear = false },
            onConfirm = { v ->
                val year = v.toInt()
                vm.update { it.copy(birthYear = year.takeIf { y -> y in (thisYear - 100)..(thisYear - 13) }) }
                editingYear = false
            },
        )
    }
}
