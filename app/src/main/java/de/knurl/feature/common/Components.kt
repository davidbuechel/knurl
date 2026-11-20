package de.knurl.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.knurl.AppContainer
import de.knurl.KnurlApp
import de.knurl.R
import de.knurl.data.WeightUnit
import de.knurl.ui.theme.Knurl

/** The app's long-lived objects. Screens use [knurlViewModel]; this is for the few app-wide reads. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as KnurlApp).container

/** ViewModels get their dependencies from the [AppContainer]; this is the one way screens create them. */
@Composable
inline fun <reified VM : ViewModel> knurlViewModel(key: String? = null, noinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(key = key) { create(container) }
}

/** The one card style: white on steel in light mode, raised graphite in dark mode. */
@Composable
fun KnurlCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    // No border: fill and space only (DesignSystem.kt rules).
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
    if (onClick != null) Card(onClick, modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = inner)
    else Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = inner)
}

/** Small uppercase label above a group, like "THIS WEEK". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Large screen title for the tabs, which have no top bar. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, eyebrow: String? = null) {
    Column(modifier.padding(top = 8.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (eyebrow != null) SectionLabel(eyebrow)
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun RowScope.StatTile(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * Minus / value / plus, big enough for sweaty hands. With [onType], tapping the value opens a
 * dialog to type it, for jumps the buttons would take too long for.
 */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    onType: (() -> Unit)? = null,
    highlighted: Boolean = false,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
        }
        Box(
            Modifier
                .weight(1f)
                .height(40.dp)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(10.dp),
                )
                .let { if (onType != null) it.clickable(onClick = onType) else it },
            contentAlignment = Alignment.Center,
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
        }
    }
}

/** Dialog to type a number. Accepts "," and "." as decimal separator. */
@Composable
fun NumberInputDialog(
    title: String,
    initial: String,
    decimal: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val parsed = text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = parsed == null,
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Simple yes/no confirmation for anything that throws data away. */
@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The bar as it is loaded: plates per side (in [unit]) in competition colours, sized by weight. */
@Composable
fun PlateBar(perSide: List<Double>, unit: WeightUnit, modifier: Modifier = Modifier) {
    val colors = Knurl.colors
    val kg = { plate: Double -> Formatting.fromUnit(plate, unit) }
    Row(modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(Modifier.width(14.dp).height(10.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
        perSide.reversed().forEach { Plate(kg(it), colors.plate(kg(it))) }
        Box(Modifier.width(72.dp).height(8.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)))
        perSide.forEach { Plate(kg(it), colors.plate(kg(it))) }
        Box(Modifier.width(14.dp).height(10.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun Plate(kg: Double, color: Color) {
    val height = (28 + kg * 1.8).coerceAtMost(76.0).dp
    Box(Modifier.padding(horizontal = 1.dp).width(12.dp).height(height).background(color, RoundedCornerShape(3.dp)))
}
