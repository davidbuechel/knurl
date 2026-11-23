package de.knurl.feature.home

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import de.knurl.R
import de.knurl.feature.progress.ProgressScreen
import androidx.compose.material.icons.rounded.Insights
import de.knurl.feature.settings.ProfileScreen
import de.knurl.feature.today.TodayScreen
import de.knurl.feature.training.TrainingScreen
import de.knurl.ui.navigation.Navigator

/**
 * Four places: what to do today, everything to train with (routines, history, exercises), how
 * it's going (statistics, body weight, calendar, achievements), and you (account, settings).
 */
enum class HomeTab(@StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Rounded.Today),
    TRAINING(R.string.tab_training, Icons.Rounded.FitnessCenter),
    PROGRESS(R.string.tab_progress, Icons.Rounded.Insights),
    PROFILE(R.string.tab_profile, Icons.Rounded.Person),
}

/**
 * The four tabs (see [HomeTab]). A tab change fades through (the old tab leaves quickly, the new one comes in), each
 * tab keeps its scroll position, and back on another tab returns to Today before it leaves the app.
 * Everything that opens on top slides (see ScreenMotion).
 */
@Composable
fun HomeScreen(go: Navigator) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.TODAY) }
    val tabs = rememberSaveableStateHolder()
    BackHandler(enabled = tab != HomeTab.TODAY) { tab = HomeTab.TODAY }
    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.98f)) togetherWith fadeOut(tween(90))
            },
            label = "tab",
            modifier = Modifier.padding(padding),
        ) { current ->
            Box(Modifier.fillMaxSize()) {
                tabs.SaveableStateProvider(current.name) {
                    when (current) {
                        HomeTab.TODAY -> TodayScreen(go, openTab = { tab = it })
                        HomeTab.TRAINING -> TrainingScreen(go)
                        HomeTab.PROGRESS -> ProgressScreen(go)
                        HomeTab.PROFILE -> ProfileScreen(go)
                    }
                }
            }
        }
    }
}
