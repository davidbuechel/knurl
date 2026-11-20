package de.knurl.ui.brand

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The mark: a geometric K whose upper arm ends in a plate. Drawn from the same geometry as the
 * launcher icon (res/drawable/ic_launcher_foreground.xml), on a 68-unit grid.
 */
@Composable
fun KnurlMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset((x - ORIGIN) * unit, (y - ORIGIN) * unit)
        val stroke = 10f * unit
        drawLine(color, p(40f, 33f), p(40f, 75f), stroke, StrokeCap.Round)
        drawLine(color, p(40f, 58f), p(63f, 35f), stroke, StrokeCap.Round)
        drawLine(color, p(50f, 50f), p(67f, 75f), stroke, StrokeCap.Round)
        drawCircle(color, radius = 7.5f * unit, center = p(68f, 31f))
    }
}

private const val GRID = 68f
private const val ORIGIN = 20f

/** The app icon as a composable: white mark on a cobalt tile. For the about row and onboarding. */
@Composable
fun KnurlAppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(28)),
        contentAlignment = Alignment.Center,
    ) {
        KnurlMark(Modifier.fillMaxSize(0.72f), color = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Loading state for an area: the mark breathes softly. The one loading language in the app. */
@Composable
fun BrandLoadingBox(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        KnurlMark(Modifier.size(40.dp).alpha(alpha))
    }
}

/** Empty state: what will appear here and how to get the first entry. */
@Composable
fun BrandEmptyState(title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else KnurlMark(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
