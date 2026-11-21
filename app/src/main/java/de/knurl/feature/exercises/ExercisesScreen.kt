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
import androidx.compose.material3.Surface
import de.knurl.feature.common.ConfirmDialog
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
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
 * with [pickForWorkout] it collects several exercises and hands them back to the screen that asked (workout, routine editor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(go: Navigator, pickForWorkout: Boolean, savedState: SavedStateHandle? = null, alreadyAdded: Set<String> = emptySet()) {
    val german = isGerman()
    val vm = knurlViewModel(key = "exercises-$pickForWorkout") {
        if (pickForWorkout) ExercisesViewModel(it.training, german, it.catalogReady, it.pickerFilter)
        else ExercisesViewModel(it.training, german, it.catalogReady)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    // Library: a tap opens the exercise. Picker: a tap selects it (several at once, kept in order),
    // the photo opens the details, and "Add" hands them all back.
    val details: (String) -> Unit = { id -> go.to(ExerciseRoute(id, pick = pickForWorkout)) }
    val open: (String) -> Unit = { id -> if (pickForWorkout) vm.toggleSelected(id) else details(id) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    if (pickForWorkout) BackHandler(enabled = state.selected.isNotEmpty()) { confirmDiscard = true }
    // An own exercise created from the picker comes back here and joins the selection.
    if (savedState != null) {
        LaunchedEffect(savedState) {
            savedState.getStateFlow<String?>(Navigator.RESULT_EXERCISE, null).collect { id ->
                if (id != null) {
                    savedState[Navigator.RESULT_EXERCISE] = null
                    vm.select(id)
                }
            }
        }
    }
    val rowContent: @Composable (ExerciseRow, Int, Int) -> Unit = { row, i, count ->
        val id = row.exercise.id
        ExerciseListRow(
            row, i, count,
            onClick = { open(id) },
            selection = if (pickForWorkout) state.selected.indexOf(id) + 1 else null,
            onDetails = if (pickForWorkout) ({ details(id) }) else null,
            added = id in alreadyAdded,
        )
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
                // Opens scrolled to the active muscle, so a remembered filter is visible at once.
                val chips = rememberLazyListState(initialFirstVisibleItemIndex = state.filter.muscle?.let { 3 + Muscle.entries.indexOf(it) } ?: 0)
                LazyRow(Modifier.padding(top = 10.dp), state = chips, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                section("fav", favoritesTitle, state.favorites, rowContent)
                section("custom", customTitle, state.custom, rowContent)
                section("recent", recentTitle, state.recent, rowContent)
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
            itemsIndexed(state.rows, key = { _, row -> "all-" + row.exercise.id }) { i, row -> rowContent(row, i, state.rows.size) }
        }
    }

    if (pickForWorkout) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.exercises_pick_title)) },
                    navigationIcon = {
                        IconButton(onClick = { if (state.selected.isNotEmpty()) confirmDiscard = true else go.back() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                )
            },
            bottomBar = {
                AnimatedVisibility(state.selected.isNotEmpty(), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    val count = state.selected.size.coerceAtLeast(1)
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Button(
                            onClick = { go.backWithResults(Navigator.RESULT_EXERCISES, state.selected) },
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                        ) { Text(pluralStringResource(R.plurals.picker_add, count, count)) }
                    }
                }
            },
        ) { padding -> content(Modifier.padding(padding)) }
    } else {
        content(Modifier)
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.picker_discard_title),
            body = pluralStringResource(R.plurals.picker_discard_body, state.selected.size, state.selected.size),
            confirm = stringResource(R.string.action_discard),
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; vm.clearSelection(); go.back() },
        )
    }
}

private fun LazyListScope.section(key: String, title: String, rows: List<ExerciseRow>, rowContent: @Composable (ExerciseRow, Int, Int) -> Unit) {
    if (rows.isEmpty()) return
    item { SectionHeader(title, Modifier.padding(top = 14.dp, bottom = 8.dp)) }
    itemsIndexed(rows, key = { _, row -> "$key-" + row.exercise.id }) { i, row -> rowContent(row, i, rows.size) }
}

/**
 * One exercise as a row; consecutive rows look like one [de.knurl.feature.common.ListGroup].
 * In the picker, [selection] is its place in the selection (0 = not chosen) and the photo has its
 * own tap: [onDetails] opens the exercise without choosing it.
 */
@Composable
private fun ExerciseListRow(
    row: ExerciseRow, index: Int, count: Int, onClick: () -> Unit,
    selection: Int? = null, onDetails: (() -> Unit)? = null,
    /** Already in the workout or routine (picker): marked, still selectable for a second round. */
    added: Boolean = false,
) {
    val chosen = selection != null && selection > 0
    ListRow(
        row.exercise.displayName(),
        modifier = Modifier.groupedItem(index, count)
            .background(if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent),
        subtitle = if (added) stringResource(R.string.picker_already_added, row.exercise.subtitle()) else row.exercise.subtitle(),
        leading = {
            val details = stringResource(R.string.picker_details)
            Box(
                if (onDetails != null) Modifier.clip(MaterialTheme.shapes.small).clickable(onClickLabel = details, onClick = onDetails) else Modifier,
            ) {
                ExerciseThumbnail(row.exercise, Modifier.size(44.dp))
                // A small "i" says the photo opens the details.
                if (onDetails != null) {
                    Icon(
                        Icons.Rounded.Info, contentDescription = details,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                    )
                }
            }
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                if (row.favorite) Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.exercises_favorites), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                row.bestOneRepMaxKg?.let { Text(stringResource(R.string.unit_e1rm, estimateText(it)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (selection != null) SelectionMark(selection)
        },
        divider = index < count - 1,
        onClick = onClick,
    )
}

/** Picker: an empty ring, or a filled one with the exercise's place in the selection. */
@Composable
private fun SelectionMark(place: Int) {
    val chosen = place > 0
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .background(if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(2.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (chosen) Text("$place", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
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
