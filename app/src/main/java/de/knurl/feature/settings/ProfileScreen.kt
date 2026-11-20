package de.knurl.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.BuildConfig
import de.knurl.R
import de.knurl.data.Settings
import de.knurl.data.repository.TrainingRepository
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.Stepper
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.brand.KnurlAppIcon

class ProfileViewModel(private val training: TrainingRepository) : ViewModel() {
    val data = training.data

    fun update(transform: (Settings) -> Settings) = training.updateSettings(transform)
}

private enum class Editing { BAR, REST, GOAL }

@Composable
fun ProfileScreen() {
    val vm = knurlViewModel { ProfileViewModel(it.training) }
    val data by vm.data.collectAsStateWithLifecycle()
    val d = data ?: return BrandLoadingBox()
    val settings = d.settings
    var editing by rememberSaveable { mutableStateOf<Editing?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(stringResource(R.string.tab_profile)) }

        item {
            KnurlCard {
                SectionLabel(stringResource(R.string.settings_training))
                SettingRow(stringResource(R.string.settings_bar_weight)) {
                    Stepper(
                        value = stringResource(R.string.unit_kg_value, Formatting.kg(settings.barKg)),
                        onMinus = { vm.update { it.copy(barKg = (it.barKg - 2.5).coerceAtLeast(0.0)) } },
                        onPlus = { vm.update { it.copy(barKg = it.barKg + 2.5) } },
                        onType = { editing = Editing.BAR },
                        modifier = Modifier.width(176.dp),
                    )
                }
                SettingRow(stringResource(R.string.settings_default_rest)) {
                    Stepper(
                        value = Formatting.duration(settings.defaultRestSec.toLong()),
                        onMinus = { vm.update { it.copy(defaultRestSec = (it.defaultRestSec - 15).coerceAtLeast(15)) } },
                        onPlus = { vm.update { it.copy(defaultRestSec = it.defaultRestSec + 15) } },
                        onType = { editing = Editing.REST },
                        modifier = Modifier.width(176.dp),
                    )
                }
                SettingRow(stringResource(R.string.settings_weekly_goal)) {
                    Stepper(
                        value = "${settings.weeklyGoal}",
                        onMinus = { vm.update { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(1)) } },
                        onPlus = { vm.update { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(7)) } },
                        onType = { editing = Editing.GOAL },
                        modifier = Modifier.width(176.dp),
                    )
                }
            }
        }

        item {
            KnurlCard {
                SectionLabel(stringResource(R.string.settings_data))
                Text(stringResource(R.string.settings_data_local), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.settings_data_count, d.workouts.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KnurlAppIcon(Modifier.size(40.dp))
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    when (editing) {
        Editing.BAR -> NumberInputDialog(stringResource(R.string.settings_bar_weight), Formatting.kg(settings.barKg), decimal = true,
            onDismiss = { editing = null }, onConfirm = { v -> vm.update { it.copy(barKg = v) }; editing = null })
        Editing.REST -> NumberInputDialog(stringResource(R.string.settings_default_rest_seconds), "${settings.defaultRestSec}", decimal = false,
            onDismiss = { editing = null }, onConfirm = { v -> vm.update { it.copy(defaultRestSec = v.toInt().coerceAtLeast(15)) }; editing = null })
        Editing.GOAL -> NumberInputDialog(stringResource(R.string.settings_weekly_goal), "${settings.weeklyGoal}", decimal = false,
            onDismiss = { editing = null }, onConfirm = { v -> vm.update { it.copy(weeklyGoal = v.toInt().coerceIn(1, 7)) }; editing = null })
        null -> Unit
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        control()
    }
}
