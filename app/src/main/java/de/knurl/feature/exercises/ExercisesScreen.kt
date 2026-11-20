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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.Add
import de.knurl.ui.navigation.CustomExerciseRoute
import androidx.compose.runtime.saveable.rememberSaveable
import de.knurl.R
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import de.knurl.feature.common.estimateText
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.ExerciseLevel
import de.knurl.domain.model.Muscle
import de.knurl.domain.search.ExerciseFilter
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.groupedItem
import de.knurl.feature.common.ScreenTitle
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
fun ExercisesScreen(go: Navigator, pickForWorkout: Boolean, savedState: SavedStateHandle? = null) {
    val german = isGerman()
    val vm = knurlViewModel(key = "exercises-$pickForWorkout") { ExercisesViewModel(it.training, german, it.catalogReady) }
    val state by vm.state.collectAsStateWithLifecycle()
    val open: (String) -> Unit = { id ->
        if (pickForWorkout) go.backWithResult(Navigator.RESULT_EXERCISE, id) else go.to(ExerciseRoute(id))
    }
    // An own exercise created from the picker comes back here and goes straight on to whoever asked.
    if (savedState != null) {
        // Only once this screen is back on top: during the way back it could not navigate yet.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(savedState) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                savedState.getStateFlow<String?>(Navigator.RESULT_EXERCISE, null).collect { id ->
                    if (id != null) {
                        savedState[Navigator.RESULT_EXERCISE] = null
                        go.backWithResult(Navigator.RESULT_EXERCISE, id)
                    }
                }
            }
        }
    }
    // The field owns its text; the list follows. Bound to the computed state, fast typing would lose letters.
    var query by rememberSaveable { mutableStateOf(state.query) }
    val setQuery: (String) -> Unit = { query = it; vm.setQuery(it) }
    val create: () -> Unit = { go.to(CustomExerciseRoute(name = query, pick = pickForWorkout)) }
    val favoritesTitle = stringResource(R.string.exercises_favorites)
    val customTitle = stringResource(R.string.exercises_custom)
    val recentTitle = stringResource(R.string.exercises_recent)

    // The keyboard gets out of the way as soon as the list is scrolled or search is pressed.
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) focusManager.clearFocus() }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(16.dp)) {
            if (!pickForWorkout) item {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(stringResource(R.string.tab_exercises), Modifier.weight(1f))
                    FilledTonalButton(onClick = create) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.custom_add))
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { setQuery("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    },
                    placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }
            item {
                LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ChoiceChip(stringResource(R.string.filter_equipment), state.filter.equipment?.let { stringResource(it.label()) }, Equipment.entries, { stringResource(it.label()) }, vm::setEquipment)
                    }
                    item {
                        ChoiceChip(stringResource(R.string.filter_level), state.filter.level?.let { stringResource(it.label()) }, ExerciseLevel.entries, { stringResource(it.label()) }, vm::setLevel)
                    }
                    item {
                        FilterChip(selected = state.filter.customOnly, onClick = vm::toggleCustomOnly, label = { Text(stringResource(R.string.filter_custom)) })
                    }
                    items(Muscle.entries) { muscle ->
                        FilterChip(selected = state.filter.muscle == muscle, onClick = { vm.toggleMuscle(muscle) }, label = { Text(stringResource(muscle.label())) })
                    }
                }
            }
            if (state.browsing) {
                section("fav", favoritesTitle, state.favorites, open)
                section("custom", customTitle, state.custom, open)
                section("recent", recentTitle, state.recent, open)
                item { SectionHeader(stringResource(R.string.exercises_all, state.rows.size), Modifier.padding(top = 14.dp, bottom = 8.dp)) }
            } else {
                item {
                    Row(Modifier.padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.exercises_found, state.rows.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        if (state.filter != ExerciseFilter()) TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.filter_clear)) }
                    }
                }
            }
            if (state.rows.isEmpty()) {
                item {
                    if (query.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            BrandEmptyState(stringResource(R.string.empty_search_title), stringResource(R.string.custom_create_body), icon = Icons.Rounded.Search)
                            Button(onClick = create) { Text(stringResource(R.string.custom_create_named, query.trim())) }
                        }
                    } else {
                        BrandEmptyState(stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_body), icon = Icons.Rounded.Search)
                    }
                }
            }
            itemsIndexed(state.rows, key = { _, row -> "all-" + row.exercise.id }) { i, row ->
                ExerciseListRow(row, i, state.rows.size, onClick = { open(row.exercise.id) })
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

private fun LazyListScope.section(key: String, title: String, rows: List<ExerciseRow>, open: (String) -> Unit) {
    if (rows.isEmpty()) return
    item { SectionHeader(title, Modifier.padding(top = 14.dp, bottom = 8.dp)) }
    itemsIndexed(rows, key = { _, row -> "$key-" + row.exercise.id }) { i, row -> ExerciseListRow(row, i, rows.size, onClick = { open(row.exercise.id) }) }
}

/** One exercise as a row; consecutive rows look like one [de.knurl.feature.common.ListGroup]. */
@Composable
private fun ExerciseListRow(row: ExerciseRow, index: Int, count: Int, onClick: () -> Unit) {
    ListRow(
        row.exercise.displayName(),
        modifier = Modifier.groupedItem(index, count),
        subtitle = row.exercise.subtitle(),
        leading = { ExerciseThumbnail(row.exercise, Modifier.size(44.dp)) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                if (row.favorite) Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.exercises_favorites), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                row.bestOneRepMaxKg?.let { Text(stringResource(R.string.unit_e1rm, estimateText(it)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        divider = index < count - 1,
        onClick = onClick,
    )
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
