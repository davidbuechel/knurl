package de.knurl.feature.achievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import de.knurl.R
import de.knurl.data.TrainingData
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.achievements.AchievementStatus
import de.knurl.domain.achievements.Achievements
import de.knurl.domain.calc.Weeks
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.brand.Medal
import de.knurl.ui.brand.tierColor
import de.knurl.ui.navigation.Navigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/** Achievements for this data, as of now. */
fun TrainingData.achievements(): List<AchievementStatus> {
    val zone = TimeZone.currentSystemDefault()
    return Achievements.evaluate(workouts, bodyWeights, settings.weeklyGoal, zone, Weeks.dateOf(Clock.System.now().toEpochMilliseconds(), zone))
}

class AchievementsViewModel(training: TrainingRepository) : ViewModel() {
    val state: StateFlow<List<AchievementStatus>?> = training.data.filterNotNull().map { it.achievements() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Every medal in ladder order: unlocked ones in their metal, the rest with the progress so far. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(go: Navigator) {
    val vm = knurlViewModel { AchievementsViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements_title)) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        val list = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        val unlocked = list.count { it.unlocked }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                KnurlCard {
                    Text(stringResource(R.string.achievements_count, unlocked, list.size), style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Box(Modifier.fillMaxWidth(unlocked / list.size.toFloat()).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
            items(list, key = { it.achievement.id }) { status ->
                Column(
                    Modifier.clickable { open = status.achievement.id }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Medal(status.achievement.kind, status.achievement.tier, status.unlocked, status.fraction)
                    Text(
                        status.achievement.title(),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (status.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        open?.let { id -> list.firstOrNull { it.achievement.id == id } }?.let { status ->
            ModalBottomSheet(onDismissRequest = { open = null }) { AchievementDetail(status) }
        }
    }
}

@Composable
fun AchievementDetail(status: AchievementStatus) {
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Medal(status.achievement.kind, status.achievement.tier, status.unlocked, status.fraction, size = 104.dp)
        Text(status.achievement.tier.label(), style = MaterialTheme.typography.labelLarge, color = tierColor(status.achievement.tier))
        Text(status.achievement.title(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(status.achievement.description(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text(
            status.unlockedAtMs?.let { stringResource(R.string.achievement_unlocked_on, Formatting.date(it)) } ?: status.achievement.progressText(status.progress),
            style = MaterialTheme.typography.titleSmall,
            color = if (status.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
        )
    }
}
