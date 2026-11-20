package de.knurl.feature.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A line over time with an area fill, three faint grid lines and the newest point emphasised.
 * The axis is rounded out to whole steps of [step], so every label is a value on the scale.
 */
@Composable
fun TrendChart(values: List<Double>, modifier: Modifier = Modifier, step: Double = 2.5, format: (Double) -> String) {
    if (values.size < 2) return
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()

    val low = floor(values.min() / step) * step
    val high = ceil(values.max() / step) * step + if (values.min() == values.max()) step else 0.0
    val mid = (low + high) / 2

    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val labelWidth = 40.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val left = labelWidth
        val right = size.width - 8.dp.toPx()
        fun y(v: Double) = (bottom - (v - low) / (high - low) * (bottom - top)).toFloat()
        fun x(i: Int) = left + i * (right - left) / (values.size - 1)

        listOf(high, mid, low).forEach { v ->
            drawLine(grid, Offset(left, y(v)), Offset(right, y(v)), 1.dp.toPx())
            val text = measurer.measure(format(v), labelStyle)
            drawText(text, topLeft = Offset(0f, y(v) - text.size.height / 2f))
        }

        val path = Path().apply { values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
        val area = Path().apply {
            addPath(path)
            lineTo(x(values.lastIndex), bottom)
            lineTo(x(0), bottom)
            close()
        }
        drawPath(area, fill)
        drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(line, radius = 5.dp.toPx(), center = Offset(x(values.lastIndex), y(values.last())))
    }
}

/** Bars from zero, one per week, the current week emphasised. */
@Composable
fun WeeklyBars(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    val current = MaterialTheme.colorScheme.primary
    val past = MaterialTheme.colorScheme.primaryContainer
    val base = MaterialTheme.colorScheme.outlineVariant
    val max = values.max().takeIf { it > 0 } ?: 1.0
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        val gap = 6.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (v / max * (size.height - 2.dp.toPx())).toFloat().coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = if (i == values.lastIndex) current else past,
                topLeft = Offset(i * (width + gap), size.height - h),
                size = Size(width, h),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
        drawLine(base, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
    }
}

/** A tiny line without axes, for a row in a list. */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val line = MaterialTheme.colorScheme.primary
    val low = values.min()
    val range = (values.max() - low).takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        val pad = 3.dp.toPx()
        fun x(i: Int) = pad + i * (size.width - 2 * pad) / (values.size - 1)
        fun y(v: Double) = (size.height - pad - (v - low) / range * (size.height - 2 * pad)).toFloat()
        val path = Path().apply { values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
        drawPath(path, line, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(line, radius = 3.dp.toPx(), center = Offset(x(values.lastIndex), y(values.last())))
    }
}

/** Bars from zero with a label under each, the last one emphasised (weeks, months). */
@Composable
fun LabeledBars(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier) {
    if (values.isEmpty()) return
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val current = MaterialTheme.colorScheme.primary
    val past = MaterialTheme.colorScheme.primaryContainer
    val base = MaterialTheme.colorScheme.outlineVariant
    val max = values.max().takeIf { it > 0 } ?: 1.0
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val labelHeight = 18.dp.toPx()
        val chart = size.height - labelHeight
        val gap = 6.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (v / max * (chart - 2.dp.toPx())).toFloat().coerceAtLeast(2.dp.toPx())
            val left = i * (width + gap)
            drawRoundRect(if (i == values.lastIndex) current else past, Offset(left, chart - h), Size(width, h), CornerRadius(4.dp.toPx()))
            labels.getOrNull(i)?.let { label ->
                val text = measurer.measure(label, labelStyle)
                drawText(text, topLeft = Offset(left + (width - text.size.width) / 2, chart + 4.dp.toPx()))
            }
        }
        drawLine(base, Offset(0f, chart), Offset(size.width, chart), 1.dp.toPx())
    }
}
