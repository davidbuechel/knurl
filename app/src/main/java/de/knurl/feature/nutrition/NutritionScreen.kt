package de.knurl.feature.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.domain.model.FoodEntry
import de.knurl.domain.model.WeightGoal
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.knurlViewModel
import de.knurl.ui.brand.BrandEmptyState
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.AddFoodRoute
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.navigation.NutritionGoalRoute
import de.knurl.ui.theme.Knurl
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * The day's food: calories as a ring against the target, protein as a bar, the weight trend that
 * says whether the target works, and what was eaten. Scan or add in one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(go: Navigator) {
    val vm = knurlViewModel { NutritionViewModel(it.training, it.nutrition) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return BrandLoadingBox()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<FoodEntry?>(null) }
    val deletedText = stringResource(R.string.deleted)
    val undoText = stringResource(R.string.action_undo)
    val day = s.day.toString()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(stringResource(R.string.tab_nutrition), Modifier.weight(1f))
                    IconButton(onClick = { go.to(NutritionGoalRoute) }) {
                        Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.nutrition_goal_title))
                    }
                }
            }
            item { DaySwitcher(s, onShift = vm::shiftDay) }

            val targets = s.targets
            if (targets == null) {
                item {
                    ListGroup {
                        ListRow(
                            stringResource(R.string.nutrition_setup_title),
                            subtitle = stringResource(R.string.nutrition_setup_body),
                            leading = { IconTile(Icons.Rounded.Restaurant) },
                            divider = false,
                            onClick = { go.to(NutritionGoalRoute) },
                        )
                    }
                }
            } else {
                item { DayCard(s.totals.kcal, targets.kcal, s.totals.proteinG, targets.proteinG, s.totals.carbsG, s.totals.fatG) }
                s.trendKgPerWeek?.let { trend -> item { TrendRow(trend, s.goal?.goal) } }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { go.to(AddFoodRoute(day, scan = true)) }, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                        Text(stringResource(R.string.food_scan), modifier = Modifier.padding(start = 8.dp))
                    }
                    FilledTonalButton(onClick = { go.to(AddFoodRoute(day)) }, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(R.string.food_add), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            item { SectionHeader(stringResource(if (s.isToday) R.string.nutrition_eaten_today else R.string.nutrition_eaten)) }
            item {
                if (s.entries.isEmpty()) {
                    BrandEmptyState(stringResource(R.string.nutrition_empty_title), stringResource(R.string.nutrition_empty_body), icon = Icons.Rounded.Restaurant)
                } else {
                    ListGroup {
                        s.entries.forEachIndexed { i, entry ->
                            ListRow(
                                entry.name,
                                subtitle = entryLine(entry),
                                trailing = { Text(stringResource(R.string.kcal_value, Formatting.kg(entry.kcal.toDouble())), style = MaterialTheme.typography.labelLarge) },
                                divider = i < s.entries.lastIndex,
                                onClick = { sheet = entry },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    sheet?.let { entry ->
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Text(entry.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Column(Modifier.padding(bottom = 24.dp)) {
                ListRow(stringResource(R.string.food_again), leading = { IconTile(Icons.Rounded.Replay) }) {
                    sheet = null; vm.again(entry, s.day)
                }
                ListRow(stringResource(R.string.action_delete), leading = { IconTile(Icons.Rounded.DeleteOutline, MaterialTheme.colorScheme.error) }, divider = false) {
                    sheet = null
                    vm.delete(entry)
                    scope.launch { if (snackbar.showSnackbar(deletedText, undoText, withDismissAction = true) == SnackbarResult.ActionPerformed) vm.restore(entry) }
                }
            }
        }
    }
}

@Composable
private fun entryLine(entry: FoodEntry): String {
    val protein = stringResource(R.string.protein_value, Formatting.kg(entry.proteinG))
    return listOfNotNull(entry.grams?.let { "${Formatting.kg(it)} g" }, protein).joinToString(" · ")
}

@Composable
private fun DaySwitcher(s: NutritionState, onShift: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.day_previous)) }
        Text(
            if (s.isToday) stringResource(R.string.today) else s.day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onShift(1) }, enabled = !s.isToday) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.day_next)) }
    }
}

/** Calories as a ring (left to eat in the middle), protein as a bar, carbs and fat in a line. */
@Composable
private fun DayCard(kcal: Int, kcalTarget: Int, protein: Double, proteinTarget: Int, carbs: Double, fat: Double) {
    val fraction by animateFloatAsState((kcal.toFloat() / kcalTarget).coerceIn(0f, 1f), tween(600), label = "kcal")
    val over = kcal > kcalTarget
    val ring = if (over) Knurl.colors.warning else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(132.dp)) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(ring, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Formatting.kg(kotlin.math.abs(kcalTarget - kcal).toDouble()), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(if (over) R.string.kcal_over else R.string.kcal_left), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.kcal_of, Formatting.kg(kcal.toDouble()), Formatting.kg(kcalTarget.toDouble())), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.nutrition_kcal_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(stringResource(R.string.nutrition_protein), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.grams_of, Formatting.kg(protein.roundToInt().toDouble()), proteinTarget), style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(
                progress = { (protein / proteinTarget).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = if (protein >= proteinTarget) Knurl.colors.success else MaterialTheme.colorScheme.primary,
                trackColor = track,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            Text(
                stringResource(R.string.nutrition_carbs_fat, Formatting.kg(carbs.roundToInt().toDouble()), Formatting.kg(fat.roundToInt().toDouble())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Whether the target works: the weight trend against the goal. */
@Composable
private fun TrendRow(trend: Double, goal: WeightGoal?) {
    val onTrack = when (goal) {
        WeightGoal.LOSE -> trend in -1.0..-0.2
        WeightGoal.GAIN -> trend in 0.1..0.5
        else -> trend in -0.2..0.2
    }
    val icon = when {
        trend > 0.05 -> Icons.AutoMirrored.Rounded.TrendingUp
        trend < -0.05 -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingFlat
    }
    ListGroup {
        ListRow(
            stringResource(R.string.trend_value, (if (trend > 0) "+" else "") + Formatting.kg(trend)),
            subtitle = stringResource(if (onTrack) R.string.trend_on_track else R.string.trend_off_track),
            leading = { IconTile(icon, if (onTrack) Knurl.colors.success else Knurl.colors.warning) },
            divider = false,
        )
    }
}
