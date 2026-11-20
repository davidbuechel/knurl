package de.knurl.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * The Knurl mark: three plates, rising, on the end of a bar. The same three bars are the app's loading and progress
 * language: they fill one after another like plates being loaded onto a bar.
 * Geometry on the 108-unit icon grid, shared with res/drawable/ic_launcher_foreground.xml.
 */

private data class Plate(val x: Float, val height: Float)

private val Plates = listOf(Plate(26f, 18f), Plate(43.5f, 34f), Plate(61f, 50f))

/** The bar end on the right of the largest plate: x 72..86, 6 units thick, on the axis. */
private const val SLEEVE_START = 72f
private const val SLEEVE_END = 86f
private const val SLEEVE_THICKNESS = 6f
private const val PLATE_WIDTH = 11f
private const val AXIS = 54f
private const val GRID_START = 22f
private const val GRID = 64f

/**
 * Draws the bar end and the three plates. [fill] says how full each plate is (0..1, grown from the
 * bar axis); the unfilled part shows in [track]. The bar end is always drawn in [color].
 */
private fun DrawScope.drawPlates(color: Color, track: Color, fill: (Int) -> Float) {
    val unit = size.minDimension / GRID
    val left = (size.width - GRID * unit) / 2
    val top = (size.height - GRID * unit) / 2
    val sleeveH = SLEEVE_THICKNESS * unit
    drawRoundRect(
        color,
        Offset(left + (SLEEVE_START - GRID_START) * unit, top + (AXIS - GRID_START) * unit - sleeveH / 2),
        Size((SLEEVE_END - SLEEVE_START) * unit, sleeveH),
        CornerRadius(sleeveH / 2),
    )
    Plates.forEachIndexed { i, plate ->
        val x = left + (plate.x - GRID_START) * unit
        val w = PLATE_WIDTH * unit
        val fullH = plate.height * unit
        val axis = top + (AXIS - GRID_START) * unit
        val radius = CornerRadius(w / 2)
        if (track.alpha > 0f) drawRoundRect(track, Offset(x, axis - fullH / 2), Size(w, fullH), radius)
        val f = fill(i).coerceIn(0f, 1f)
        if (f > 0f) {
            val h = (w + (fullH - w) * f).coerceAtMost(fullH)
            drawRoundRect(color, Offset(x, axis - h / 2), Size(w, h), radius, alpha = if (f < 0.02f) f / 0.02f else 1f)
        }
    }
}

@Composable
fun KnurlMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) { drawPlates(color, Color.Transparent) { 1f } }
}

/** The app icon as a composable: white plates on a cobalt tile. */
@Composable
fun KnurlAppIcon(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(28)), contentAlignment = Alignment.Center) {
        KnurlMark(Modifier.fillMaxSize(0.78f), color = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Indeterminate loading: the plates are loaded one by one, held for a beat, then taken off
 * together. One cycle is 1.4 s. Replaces every spinner in the app.
 */
@Composable
fun KnurlLoader(modifier: Modifier = Modifier.size(44.dp), color: Color = MaterialTheme.colorScheme.primary) {
    val t by rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val track = color.copy(alpha = 0.18f)
    Canvas(modifier.semantics { contentDescription = "…" }) {
        drawPlates(color, track) { i ->
            // 0.00–0.60: plates go on one after another; 0.60–0.80: hold; 0.80–1.00: all come off.
            val on = ((t - i * 0.18f) / 0.24f).coerceIn(0f, 1f)
            val off = ((t - 0.80f) / 0.20f).coerceIn(0f, 1f)
            easeOut(on) * (1f - off)
        }
    }
}

/**
 * Determinate progress in the same shape: [progress] 0..1 fills the plates left to right, each a
 * third of the way. Reaching 1 gives a short springy pop.
 */
@Composable
fun KnurlProgress(progress: Float, modifier: Modifier = Modifier.size(28.dp), color: Color = MaterialTheme.colorScheme.primary) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.6f, stiffness = 300f), label = "progress")
    Canvas(modifier.semantics { contentDescription = "${(progress * 100).toInt()} %" }) {
        drawPlates(color, color.copy(alpha = 0.18f)) { i -> (animated * 3f - i) }
    }
}

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)

/** Loading state for an area. */
@Composable
fun BrandLoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { KnurlLoader() }
}

/** A request running over visible content: dims it, blocks taps and shows the loader. */
@Composable
fun BrandBusyOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(22.dp)) { KnurlLoader() }
    }
}

/** A request running on a button: the label makes room for the small loader. */
@Composable
fun BrandButtonLabel(text: String, busy: Boolean) {
    if (busy) KnurlLoader(Modifier.size(22.dp), color = LocalContentColor.current) else Text(text)
}

/** Empty state: what will appear here and how to get the first entry. */
@Composable
fun BrandEmptyState(title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else KnurlMark(Modifier.size(30.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
