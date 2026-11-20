package de.knurl.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import de.knurl.R
import de.knurl.data.firebase.AuthRepository
import de.knurl.data.firebase.AuthState
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.Volume
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.StatTile
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.tonnageText
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.brand.KnurlAppIcon
import de.knurl.ui.navigation.AccountRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.SettingsRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class ProfileState(
    val anonymous: Boolean,
    val email: String?,
    val workouts: Int,
    val totalVolumeKg: Double,
    val firstWorkoutMs: Long?,
)

class ProfileViewModel(training: TrainingRepository, private val auth: AuthRepository) : ViewModel() {
    val state: StateFlow<ProfileState?> = combine(training.data.filterNotNull(), auth.state) { data, authState ->
        ProfileState(
            anonymous = (authState as? AuthState.SignedIn)?.anonymous ?: true,
            email = auth.email(),
            workouts = data.workouts.size,
            totalVolumeKg = data.workouts.sumOf(Volume::of),
            firstWorkoutMs = data.workouts.minOfOrNull { it.startedAtMs },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProfileScreen(go: Navigator) {
    val vm = knurlViewModel { ProfileViewModel(it.training, it.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(stringResource(R.string.tab_profile)) }

        item {
            KnurlCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KnurlAppIcon(Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (s.anonymous) stringResource(R.string.account_anonymous) else s.email ?: stringResource(R.string.account_linked),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(if (s.anonymous) R.string.account_anonymous_hint else R.string.account_linked_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AccountActions(go, s.anonymous)
            }
        }

        item {
            KnurlCard {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("${s.workouts}", stringResource(R.string.profile_workouts))
                    StatTile(tonnageText(s.totalVolumeKg), stringResource(R.string.profile_total_volume))
                    StatTile(s.firstWorkoutMs?.let { Formatting.monthYear(it) } ?: "–", stringResource(R.string.profile_since))
                }
            }
        }

        item {
            KnurlCard {
                LinkRow(Icons.Outlined.Settings, stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle)) {
                    go.to(SettingsRoute)
                }
            }
        }
    }
}

@Composable
private fun AccountActions(go: Navigator, anonymous: Boolean) {
    if (anonymous) {
        Button(onClick = { go.to(AccountRoute) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_secure_cta)) }
    } else {
        OutlinedButton(onClick = { go.to(AccountRoute) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_manage)) }
    }
}
