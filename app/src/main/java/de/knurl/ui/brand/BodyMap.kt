package de.knurl.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import de.knurl.domain.calc.LoadLevel
import de.knurl.domain.model.Muscle

/*
 * A calm, geometric body (front and back) in the brand's plate language: rounded blocks, no
 * anatomy drawing. Each muscle is one or two blocks, filled by how much it was trained.
 * Coordinates are in a 100 × 200 box per figure.
 */

private data class Region(val muscle: Muscle, val rect: Rect, val round: Float = 5f)

private fun r(l: Float, t: Float, rr: Float, b: Float) = Rect(l, t, rr, b)

/** The silhouette both figures share. */
private val Silhouette = listOf(
    r(40f, 3f, 60f, 25f) to 10f, // head
    r(45f, 22f, 55f, 32f) to 3f, // neck
    r(29f, 30f, 71f, 100f) to 12f, // torso
    r(17f, 32f, 30f, 70f) to 6f, r(70f, 32f, 83f, 70f) to 6f, // upper arms
    r(13f, 70f, 27f, 106f) to 6f, r(73f, 70f, 87f, 106f) to 6f, // forearms
    r(31f, 96f, 69f, 116f) to 8f, // hips
    r(31f, 112f, 49.5f, 162f) to 8f, r(50.5f, 112f, 69f, 162f) to 8f, // thighs
    r(33f, 162f, 48f, 196f) to 7f, r(52f, 162f, 67f, 196f) to 7f, // shins
)

private val Front = listOf(
    Region(Muscle.SHOULDERS, r(20f, 31f, 32f, 43f), 6f), Region(Muscle.SHOULDERS, r(68f, 31f, 80f, 43f), 6f),
    Region(Muscle.CHEST, r(33f, 36f, 49f, 55f)), Region(Muscle.CHEST, r(51f, 36f, 67f, 55f)),
    Region(Muscle.CORE, r(40f, 58f, 60f, 95f)),
    Region(Muscle.BICEPS, r(19f, 45f, 28f, 67f), 4f), Region(Muscle.BICEPS, r(72f, 45f, 81f, 67f), 4f),
    Region(Muscle.FOREARMS, r(15f, 73f, 25f, 101f), 4f), Region(Muscle.FOREARMS, r(75f, 73f, 85f, 101f), 4f),
    Region(Muscle.QUADS, r(33f, 116f, 48f, 157f)), Region(Muscle.QUADS, r(52f, 116f, 67f, 157f)),
)

private val Back = listOf(
    Region(Muscle.SHOULDERS, r(20f, 31f, 32f, 43f), 6f), Region(Muscle.SHOULDERS, r(68f, 31f, 80f, 43f), 6f),
    Region(Muscle.BACK, r(33f, 34f, 67f, 82f), 8f),
    Region(Muscle.TRICEPS, r(19f, 45f, 28f, 67f), 4f), Region(Muscle.TRICEPS, r(72f, 45f, 81f, 67f), 4f),
    Region(Muscle.FOREARMS, r(15f, 73f, 25f, 101f), 4f), Region(Muscle.FOREARMS, r(75f, 73f, 85f, 101f), 4f),
    Region(Muscle.GLUTES, r(33f, 98f, 49f, 114f), 7f), Region(Muscle.GLUTES, r(51f, 98f, 67f, 114f), 7f),
    Region(Muscle.HAMSTRINGS, r(33f, 117f, 48f, 157f)), Region(Muscle.HAMSTRINGS, r(52f, 117f, 67f, 157f)),
    Region(Muscle.CALVES, r(35f, 164f, 47f, 190f), 6f), Region(Muscle.CALVES, r(53f, 164f, 65f, 190f), 6f),
)

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

/**
 * Front and back side by side. [levels] colours each muscle; tapping one reports it, and
 * [selected] gets an outline.
 */
@Composable
fun BodyMap(levels: Map<Muscle, LoadLevel>, modifier: Modifier = Modifier, selected: Muscle? = null, onSelect: (Muscle) -> Unit = {}) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.onSurface
    val colors = LoadLevel.entries.associateWith { loadColor(it) }
    Canvas(
        modifier.fillMaxWidth().aspectRatio(1.15f).pointerInput(levels) {
            detectTapGestures { tap ->
                val scale = size.height / 200f
                val figureWidth = 100f * scale
                val gap = (size.width - 2 * figureWidth) / 3
                listOf(Front to gap, Back to gap * 2 + figureWidth).forEach { (regions, left) ->
                    val x = (tap.x - left) / scale
                    val y = tap.y / scale
                    regions.firstOrNull { it.rect.inflate(2f).contains(Offset(x, y)) }?.let { onSelect(it.muscle); return@detectTapGestures }
                }
            }
        },
    ) {
        val scale = size.height / 200f
        val figureWidth = 100f * scale
        val gap = (size.width - 2 * figureWidth) / 3
        listOf(Front to gap, Back to gap * 2 + figureWidth).forEach { (regions, left) ->
            Silhouette.forEach { (rect, round) -> block(rect, round, left, scale, base) }
            regions.forEach { region ->
                val color = colors.getValue(levels[region.muscle] ?: LoadLevel.NONE)
                block(region.rect, region.round, left, scale, color)
                if (region.muscle == selected) block(region.rect, region.round, left, scale, outline, stroke = 1.6f)
            }
        }
    }
}

private fun DrawScope.block(rect: Rect, round: Float, left: Float, scale: Float, color: Color, stroke: Float? = null) {
    val topLeft = Offset(left + rect.left * scale, rect.top * scale)
    val size = Size(rect.width * scale, rect.height * scale)
    val radius = CornerRadius(round * scale)
    if (stroke == null) drawRoundRect(color, topLeft, size, radius)
    else drawRoundRect(color, topLeft, size, radius, style = Stroke(stroke * scale))
}
