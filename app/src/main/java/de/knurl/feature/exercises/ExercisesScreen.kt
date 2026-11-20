package de.knurl.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.ExerciseLevel
import de.knurl.domain.model.Muscle
import de.knurl.domain.search.ExerciseFilter
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionLabel
import de.knurl.feature.common.displayName
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.label
import de.knurl.feature.common.subtitle
import de.knurl.feature.common.weightText
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.navigation.ExerciseRoute
import de.knurl.ui.navigation.Navigator

/**
 * The exercise library. One screen for both uses: as a tab it opens an exercise's details,
 * with [pickForWorkout] it hands the tapped exercise back to the screen that asked (workout, routine editor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(go: Navigator, pickForWorkout: Boolean) {
    val german = isGerman()
    val vm = knurlViewModel(key = "exercises-$pickForWorkout") { ExercisesViewModel(it.training, german, it.catalogReady) }
    val state by vm.state.collectAsStateWithLifecycle()
    val open: (String) -> Unit = { id ->
        if (pickForWorkout) go.backWithResult(Navigator.RESULT_EXERCISE, id) else go.to(ExerciseRoute(id))
    }
    val favoritesTitle = stringResource(R.string.exercises_favorites)
    val recentTitle = stringResource(R.string.exercises_recent)

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
                    trailingIcon = {
                        if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    },
                    placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
                    shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ChoiceChip(stringResource(R.string.filter_equipment), state.filter.equipment?.let { stringResource(it.label()) }, Equipment.entries, { stringResource(it.label()) }, vm::setEquipment)
                    }
                    item {
                        ChoiceChip(stringResource(R.string.filter_level), state.filter.level?.let { stringResource(it.label()) }, ExerciseLevel.entries, { stringResource(it.label()) }, vm::setLevel)
                    }
                    items(Muscle.entries) { muscle ->
                        FilterChip(selected = state.filter.muscle == muscle, onClick = { vm.toggleMuscle(muscle) }, label = { Text(stringResource(muscle.label())) })
                    }
                }
            }
            if (state.browsing) {
                section("fav", favoritesTitle, state.favorites, open)
                section("recent", recentTitle, state.recent, open)
                item { SectionLabel(stringResource(R.string.exercises_all, state.rows.size), Modifier.padding(top = 8.dp)) }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.exercises_found, state.rows.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        if (state.filter != ExerciseFilter()) TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.filter_clear)) }
                    }
                }
            }
            if (state.rows.isEmpty()) {
                item { BrandEmptyState(stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_body), icon = Icons.Rounded.Search) }
            }
            items(state.rows, key = { "all-" + it.exercise.id }) { row -> ExerciseRowCard(row, onClick = { open(row.exercise.id) }) }
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

private fun LazyListScope.section(key: String, title: String, rows: List<ExerciseRow>, open: (String) -> Unit) {
    if (rows.isEmpty()) return
    item { SectionLabel(title, Modifier.padding(top = 8.dp)) }
    items(rows, key = { "$key-" + it.exercise.id }) { row -> ExerciseRowCard(row, onClick = { open(row.exercise.id) }) }
}

@Composable
private fun ExerciseRowCard(row: ExerciseRow, onClick: () -> Unit) {
    KnurlCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseThumbnail(row.exercise)
            Column(Modifier.weight(1f)) {
                Text(row.exercise.displayName(), style = MaterialTheme.typography.titleMedium)
                Text(row.exercise.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (row.favorite) Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                row.bestOneRepMaxKg?.let { Text(stringResource(R.string.unit_e1rm, weightText(it)), style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

/** A chip that opens a list of choices, with "all" to clear it. */
@Composable
private fun <T> ChoiceChip(label: String, selectedLabel: String?, options: List<T>, optionLabel: @Composable (T) -> String, onSelect: (T?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selectedLabel != null,
            onClick = { open = true },
            label = { Text(selectedLabel ?: label) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_all)) }, onClick = { open = false; onSelect(null) })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}
