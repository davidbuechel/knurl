package de.knurl.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.knurl.R
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.TextButton
import de.knurl.domain.model.BodyWeight
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.TrendChart
import de.knurl.feature.common.weightNumber
import de.knurl.feature.common.weightText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/** Latest body weight, its trend, and a button to log today's. */
@Composable
fun BodyWeightCard(entries: List<BodyWeight>, onAdd: (Double) -> Unit) {
    val unit = LocalWeightUnit.current
    var adding by rememberSaveable { mutableStateOf(false) }
    KnurlCard {
        val latest = entries.lastOrNull()
        if (latest == null) {
            Text(stringResource(R.string.body_weight_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(weightText(latest.weightKg), style = MaterialTheme.typography.displaySmall)
            val first = entries.takeLast(12).first()
            if (entries.size > 1) {
                val change = latest.weightKg - first.weightKg
                Text(
                    stringResource(R.string.body_weight_change, (if (change >= 0) "+" else "−") + weightText(kotlin.math.abs(change)), Formatting.date(first.atMs)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TrendChart(entries.takeLast(12).map { Formatting.toUnit(it.weightKg, unit) }, step = 1.0, format = { Formatting.kg(it) })
            }
        }
        FilledTonalButton(onClick = { adding = true }) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(R.string.body_weight_add))
        }
    }
    if (adding) {
        NumberInputDialog(
            stringResource(R.string.body_weight_in, Formatting.symbol(unit)),
            entries.lastOrNull()?.let { weightNumber(it.weightKg) } ?: "",
            decimal = true,
            range = Math.round(Formatting.toUnit(20.0, unit)).toDouble()..Math.round(Formatting.toUnit(400.0, unit)).toDouble(),
            onDismiss = { adding = false },
            onConfirm = { value ->
                onAdd(Formatting.fromUnit(value, unit))
                adding = false
            },
        )
    }
}

/** A month at a glance: every training day is a filled dot. */
@Composable
fun TrainingCalendarCard(trainingDays: Set<LocalDate>, onRecap: (YearMonth) -> Unit = {}) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val shown = YearMonth.parse(month)
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    val inMonth = trainingDays.count { YearMonth.from(it) == shown }
    KnurlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The screen is already called Calendar: the card names the month and how many days.
            Column(Modifier.weight(1f)) {
                Text("${shown.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${shown.year}", style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.calendar_days, inMonth, inMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { month = shown.minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous))
            }
            IconButton(onClick = { month = shown.plusMonths(1).toString() }, enabled = shown < YearMonth.now()) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val leading = shown.atDay(1).dayOfWeek.value - 1
        val cells = leading + shown.lengthOfMonth()
        for (week in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (column in 0 until 7) {
                    val dayNumber = week * 7 + column - leading + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..shown.lengthOfMonth()) {
                            val date = shown.atDay(dayNumber)
                            val trained = date in trainingDays
                            Box(
                                Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                                    .background(if (trained) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$dayNumber",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (date == today) FontWeight.Black else FontWeight.Normal,
                                    color = when {
                                        trained -> MaterialTheme.colorScheme.onPrimary
                                        date == today -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (inMonth > 0) {
            TextButton(onClick = { onRecap(shown) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.recap_open), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
