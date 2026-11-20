package de.knurl.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.knurl.domain.model.Muscle
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.displayName
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.label
import de.knurl.feature.common.subtitle
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.navigation.ExerciseRoute
import de.knurl.ui.navigation.Navigator

/**
 * The exercise library. One screen for both uses: as a tab it opens an exercise's details,
 * with [pickForWorkout] it adds the tapped exercise to the running workout and goes back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(go: Navigator, pickForWorkout: Boolean) {
    val german = isGerman()
    val vm = knurlViewModel(key = "exercises-$pickForWorkout") { ExercisesViewModel(it.training, german) }
    val state by vm.state.collectAsStateWithLifecycle()

    val content: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!pickForWorkout) item { ScreenTitle(stringResource(R.string.tab_exercises)) }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.exercises_search)) },
                    shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Muscle.entries) { muscle ->
                        FilterChip(
                            selected = state.muscle == muscle,
                            onClick = { vm.toggleMuscle(muscle) },
                            label = { Text(stringResource(muscle.label())) },
                        )
                    }
                }
            }
            if (state.rows.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_body)) }
            }
            items(state.rows, key = { it.exercise.id }) { row ->
                KnurlCard(onClick = {
                    if (pickForWorkout) {
                        vm.addToWorkout(row.exercise.id)
                        go.back()
                    } else {
                        go.to(ExerciseRoute(row.exercise.id))
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.exercise.displayName(), style = MaterialTheme.typography.titleMedium)
                            Text(row.exercise.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        row.bestOneRepMaxKg?.let {
                            Text(stringResource(R.string.unit_e1rm, Formatting.kg(it)), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (pickForWorkout) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.exercises_pick_title)) },
                    navigationIcon = {
                        IconButton(onClick = go::back) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                )
            },
        ) { padding -> content(Modifier.padding(padding)) }
    } else {
        content(Modifier)
    }
}
