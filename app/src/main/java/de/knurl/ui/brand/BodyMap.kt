package de.knurl.ui.brand

import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.knurl.domain.calc.LoadLevel
import de.knurl.domain.model.Muscle

/** Fill for a load level: the more, the deeper the cobalt. */
@Composable
fun loadColor(level: LoadLevel): Color {
    val primary = MaterialTheme.colorScheme.primary
    return when (level) {
        LoadLevel.NONE -> MaterialTheme.colorScheme.surfaceContainerHighest
        LoadLevel.LOW -> primary.copy(alpha = 0.32f)
        LoadLevel.TARGET -> primary.copy(alpha = 0.7f)
        LoadLevel.HIGH -> primary
    }
}

/** A body part ready to draw and to hit-test, in source coordinates. */
private class Shape(val muscle: Muscle?, val path: Path, val region: Region)

/** [bounds]: the outline's box, so the figures fill the space instead of their source sheet. */
private class Figure(val outline: Path, val shapes: List<Shape>, val bounds: androidx.compose.ui.geometry.Rect)

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

private fun figure(outline: String, parts: List<BodyPart>): Figure {
    val clip = Region(0, 0, (BodyOutlines.WIDTH * 2).toInt(), BodyOutlines.HEIGHT.toInt())
    val shapes = parts.flatMap { part ->
        part.paths.map { d ->
            val path = parse(d)
            Shape(part.muscle, path, Region().apply { setPath(path.asAndroidPath(), clip) })
        }
    }
    val path = parse(outline)
    return Figure(path, shapes, path.getBounds())
}

/** Parsed once: about 160 paths. */
private val Figures by lazy {
    listOf(
        figure(BodyOutlines.FRONT_OUTLINE, BodyOutlines.front),
        figure(BodyOutlines.BACK_OUTLINE, BodyOutlines.back),
    )
}

/** Space between the two figures, as a share of one figure's width. */
private const val GAP = 0.18f

/** One scale for both figures, cropped to their outlines and centred; returns scale and each figure's left edge. */
private fun layout(figures: List<Figure>, width: Float, height: Float): Pair<Float, List<Float>> {
    val figureWidths = figures.map { it.bounds.width }
    val total = figureWidths.sum() + figureWidths.first() * GAP
    val tallest = figures.maxOf { it.bounds.height }
    val scale = minOf(width / total, height / tallest)
    var x = (width - total * scale) / 2
    val lefts = figureWidths.map { w -> x.also { x += (w + figureWidths.first() * GAP) * scale } }
    return scale to lefts
}

/**
 * Front and back side by side, drawn from anatomical outlines. [levels] colours each muscle by
 * how much it was trained; tapping one reports it, and [selected] gets a strong outline.
 */
@Composable
fun BodyMap(levels: Map<Muscle, LoadLevel>, modifier: Modifier = Modifier, selected: Muscle? = null, onSelect: (Muscle) -> Unit = {}) {
    val figures = remember { Figures }
    // Exactly as tall as the figures need: no empty band below them.
    val ratio = remember(figures) { (figures.sumOf { it.bounds.width.toDouble() } + figures.first().bounds.width * GAP) / figures.maxOf { it.bounds.height } }
    val body = MaterialTheme.colorScheme.surfaceContainerHigh
    val line = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val strong = MaterialTheme.colorScheme.onSurface
    val colors = LoadLevel.entries.associateWith { loadColor(it) }

    Canvas(
        modifier.fillMaxWidth().aspectRatio(ratio.toFloat()).pointerInput(levels) {
            detectTapGestures { tap ->
                val (scale, lefts) = layout(figures, size.width.toFloat(), size.height.toFloat())
                figures.forEachIndexed { i, fig ->
                    val x = (tap.x - lefts[i]) / scale + fig.bounds.left
                    val y = tap.y / scale + fig.bounds.top
                    fig.shapes.firstOrNull { it.muscle != null && it.region.contains(x.toInt(), y.toInt()) }
                        ?.let { onSelect(it.muscle!!); return@detectTapGestures }
                }
            }
        },
    ) {
        val (scale, lefts) = layout(figures, size.width, size.height)
        val hairline = 1.dp.toPx() / scale
        figures.forEachIndexed { i, fig ->
            translate(left = lefts[i]) {
                withTransform({ scale(scale, scale, pivot = Offset.Zero); translate(left = -fig.bounds.left, top = -fig.bounds.top) }) {
                    drawFigure(fig, levels, colors, body, line, strong, selected, hairline)
                }
            }
        }
    }
}

private fun DrawScope.drawFigure(
    fig: Figure,
    levels: Map<Muscle, LoadLevel>,
    colors: Map<LoadLevel, Color>,
    body: Color,
    line: Color,
    strong: Color,
    selected: Muscle?,
    hairline: Float,
) {
    drawPath(fig.outline, body)
    fig.shapes.forEach { shape ->
        val fill = shape.muscle?.let { colors.getValue(levels[it] ?: LoadLevel.NONE) } ?: body
        drawPath(shape.path, fill)
        drawPath(shape.path, line, style = Stroke(hairline))
    }
    drawPath(fig.outline, line, style = Stroke(hairline * 1.4f))
    if (selected != null) {
        fig.shapes.filter { it.muscle == selected }.forEach { drawPath(it.path, strong, style = Stroke(hairline * 2.4f)) }
    }
}
