package de.knurl.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import de.knurl.R
import de.knurl.domain.calc.Nutrition
import de.knurl.domain.model.Food
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.groupedItem
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.navigation.Navigator
import kotlin.math.roundToInt

/**
 * Adding food to a day: scan a barcode, search (own foods first, then Open Food Facts), enter just
 * calories, or type a food in. Picking a food asks for the amount, with the result shown live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(go: Navigator, day: String, scanOnOpen: Boolean) {
    val german = isGerman()
    val vm = knurlViewModel(key = "add-food-$day") { AddFoodViewModel(it.nutrition, german, day) }
    val state by vm.state.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.food_offline)
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<Food?>(null) }
    var quickAdd by rememberSaveable { mutableStateOf(false) }
    var ownFood by remember { mutableStateOf<String?>(null) } // barcode or "" for a food without one
    var scannedOnce by rememberSaveable { mutableStateOf(false) }

    fun startScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(vm::lookUp) }
    }
    LaunchedEffect(scanOnOpen) { if (scanOnOpen && !scannedOnce) { scannedOnce = true; startScan() } }

    // A scanned product opens the amount; an unknown one offers to type it in.
    LaunchedEffect(scan) {
        when (val s = scan) {
            is ScanResult.Found -> { picked = s.food; vm.clearScan() }
            is ScanResult.NotFound -> { ownFood = s.barcode; vm.clearScan() }
            ScanResult.Failed -> { snackbar.showSnackbar(failedText); vm.clearScan() }
            else -> Unit
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) focus.clearFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.food_add_title)) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; vm.setQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = ""; vm.setQuery("") }) { Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.action_clear)) }
                        else IconButton(onClick = ::startScan) { Icon(Icons.Rounded.QrCodeScanner, contentDescription = stringResource(R.string.food_scan)) }
                    },
                    placeholder = { Text(stringResource(R.string.food_search_hint)) },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                )
            }
            item {
                ListGroup(Modifier.padding(top = 12.dp)) {
                    ListRow(stringResource(R.string.food_quick), subtitle = stringResource(R.string.food_quick_hint), leading = { IconTile(Icons.Rounded.Bolt) }) { quickAdd = true }
                    ListRow(stringResource(R.string.food_own), subtitle = stringResource(R.string.food_own_hint), leading = { IconTile(Icons.Rounded.EditNote) }, divider = false) { ownFood = "" }
                }
            }
            if (scan == ScanResult.LookingUp) {
                item { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            }

            if (state.saved.isNotEmpty()) {
                item { SectionHeader(stringResource(if (state.query.isBlank()) R.string.food_recent else R.string.food_yours), Modifier.padding(top = 18.dp, bottom = 8.dp)) }
                itemsIndexed(state.saved, key = { _, f -> "saved-" + f.id }) { i, food ->
                    FoodRow(food, Modifier.groupedItem(i, state.saved.size), divider = i < state.saved.lastIndex) { picked = food }
                }
            }

            when (val o = state.online) {
                OnlineSearch.Idle -> Unit
                OnlineSearch.Loading -> item { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
                OnlineSearch.Failed -> item {
                    Text(stringResource(R.string.food_offline), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp))
                }
                is OnlineSearch.Done -> {
                    val savedIds = state.saved.map { it.id }.toSet()
                    val found = o.foods.filterNot { it.id in savedIds }
                    item { SectionHeader(stringResource(R.string.food_database), Modifier.padding(top = 18.dp, bottom = 8.dp)) }
                    if (found.isEmpty()) item {
                        Text(stringResource(R.string.food_nothing_found), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    itemsIndexed(found, key = { _, f -> "off-" + f.id }) { i, food ->
                        FoodRow(food, Modifier.groupedItem(i, found.size), divider = i < found.lastIndex) { picked = food }
                    }
                    item {
                        Text(stringResource(R.string.food_source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                    }
                }
            }
        }
    }

    picked?.let { food ->
        AmountSheet(food, onDismiss = { picked = null }) { grams ->
            vm.log(food, grams)
            picked = null
            go.back()
        }
    }
    if (quickAdd) {
        QuickAddDialog(onDismiss = { quickAdd = false }) { name, kcal, protein ->
            vm.quickAdd(name, kcal, protein)
            quickAdd = false
            go.back()
        }
    }
    ownFood?.let { barcode ->
        OwnFoodDialog(barcode.ifBlank { null }, onDismiss = { ownFood = null }) { name, kcal, protein, carbs, fat ->
            picked = vm.ownFood(name, kcal, protein, carbs, fat, barcode.ifBlank { null })
            ownFood = null
        }
    }
}

@Composable
private fun FoodRow(food: Food, modifier: Modifier, divider: Boolean, onClick: () -> Unit) {
    ListRow(
        food.name,
        modifier = modifier,
        subtitle = listOfNotNull(
            food.brand.takeIf { it.isNotBlank() },
            stringResource(R.string.food_per_100, Formatting.kg(food.kcalPer100.roundToInt().toDouble()), Formatting.kg(food.proteinPer100)),
        ).joinToString(" · "),
        trailing = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        divider = divider,
        onClick = onClick,
    )
}

/** How much: grams in big type with steps, quick picks (100 g, one portion), the result live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSheet(food: Food, onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var grams by rememberSaveable(food.id) { mutableDoubleStateOf(food.servingG ?: 100.0) }
    var typing by remember { mutableStateOf(false) }
    val preview = Nutrition.portion(food, grams, "", "", 0)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(food.name, style = MaterialTheme.typography.titleLarge)
                if (food.brand.isNotBlank()) Text(food.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(onClick = { grams = (grams - 10).coerceAtLeast(5.0) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { typing = true }) {
                        Text("${Formatting.kg(grams)} g", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                FilledTonalIconButton(onClick = { grams = (grams + 10).coerceAtMost(5000.0) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = grams == 100.0, onClick = { grams = 100.0 }, label = { Text("100 g") })
                food.servingG?.let { serving ->
                    FilterChip(selected = grams == serving, onClick = { grams = serving }, label = { Text(stringResource(R.string.food_portion, Formatting.kg(serving))) })
                }
            }
            Text(
                listOfNotNull(
                    stringResource(R.string.kcal_value, Formatting.kg(preview.kcal.toDouble())),
                    stringResource(R.string.protein_value, Formatting.kg(preview.proteinG)),
                    preview.carbsG?.let { stringResource(R.string.carbs_value, Formatting.kg(it)) },
                    preview.fatG?.let { stringResource(R.string.fat_value, Formatting.kg(it)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = { onAdd(grams) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.food_add_amount)) }
        }
    }
    if (typing) {
        NumberInputDialog(
            stringResource(R.string.food_amount_title), Formatting.kg(grams), decimal = true, range = 1.0..5000.0,
            onDismiss = { typing = false }, onConfirm = { grams = it; typing = false },
        )
    }
}

/** Just a number for calories and protein, e.g. for a restaurant meal. */
@Composable
private fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (String, Int, Double) -> Unit) {
    val defaultName = stringResource(R.string.food_quick)
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    val kcalValue = kcal.toIntOrNull()?.takeIf { it in 1..20000 }
    val proteinValue = protein.replace(',', '.').ifBlank { "0" }.toDoubleOrNull()?.takeIf { it in 0.0..2000.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.food_quick)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(160) }, label = { Text(stringResource(R.string.food_name_optional)) }, singleLine = true)
                OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.nutrition_kcal_label)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(protein, { protein = it.take(6) }, label = { Text(stringResource(R.string.protein_grams)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name.trim().ifBlank { defaultName }, kcalValue!!, proteinValue!!) }, enabled = kcalValue != null && proteinValue != null) {
                Text(stringResource(R.string.food_add_amount))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** A food typed in from the label: name and values per 100 g. */
@Composable
private fun OwnFoodDialog(barcode: String?, onDismiss: () -> Unit, onDone: (String, Double, Double, Double?, Double?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    fun num(text: String, max: Double) = text.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..max }
    val ok = name.isNotBlank() && num(kcal, 1000.0) != null && num(protein, 100.0) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.food_own)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (barcode != null) Text(stringResource(R.string.food_not_found, barcode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(name, { name = it.take(120) }, label = { Text(stringResource(R.string.food_name)) }, singleLine = true)
                Text(stringResource(R.string.food_per_100_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(kcal, { kcal = it.take(6) }, label = { Text("kcal") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(protein, { protein = it.take(5) }, label = { Text(stringResource(R.string.nutrition_protein)) }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(carbs, { carbs = it.take(5) }, label = { Text(stringResource(R.string.nutrition_carbs)) }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(fat, { fat = it.take(5) }, label = { Text(stringResource(R.string.nutrition_fat)) }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(name, num(kcal, 1000.0)!!, num(protein, 100.0)!!, num(carbs, 100.0), num(fat, 100.0)) }, enabled = ok) {
                Text(stringResource(R.string.action_next))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
