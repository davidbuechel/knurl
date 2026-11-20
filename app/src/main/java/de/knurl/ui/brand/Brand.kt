package de.knurl.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The knurl pattern: the diamond cross-hatch on a barbell's grip. Used in exactly three places:
 * app icon, loading state and empty states. Never as a background texture.
 */
@Composable
fun KnurlBand(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
    phase: Float = 0f,
) {
    Canvas(modifier.height(16.dp)) {
        val step = 7.dp.toPx()
        val stroke = 1.5.dp.toPx()
        val shift = phase * step * 2
        clipRect {
            var x = -size.height - step * 2 + shift
            while (x < size.width + size.height) {
                drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), stroke)
                drawLine(color, Offset(x, 0f), Offset(x + size.height, size.height), stroke)
                x += step
            }
        }
    }
}

/** Loading state for an area: the knurl band slides like a bar being turned in the hand. */
@Composable
fun BrandLoadingBox(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "knurl").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        KnurlBand(Modifier.width(160.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), phase = phase)
    }
}

/** Empty state: what will appear here and how to get the first entry. */
@Composable
fun BrandEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KnurlBand(Modifier.width(96.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The mark: a cobalt tile with the knurl hatch, for headers and the about screen. */
@Composable
fun KnurlMark(modifier: Modifier = Modifier) {
    val tile = MaterialTheme.colorScheme.primary
    val hatch = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    Canvas(modifier) {
        drawRoundRect(tile, cornerRadius = CornerRadius(size.minDimension * 0.28f))
        val inset = size.minDimension * 0.22f
        val step = size.minDimension * 0.12f
        val stroke = size.minDimension * 0.035f
        clipRect(inset, inset, size.width - inset, size.height - inset) {
            var x = inset - size.height
            while (x < size.width) {
                drawLine(hatch, Offset(x, size.height), Offset(x + size.height, 0f), stroke)
                drawLine(hatch, Offset(x, 0f), Offset(x + size.height, size.height), stroke)
                x += step
            }
        }
    }
}
