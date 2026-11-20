package de.knurl.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.knurl.domain.achievements.AchievementKind
import de.knurl.domain.achievements.Tier
import de.knurl.ui.theme.Knurl

@Composable
fun tierColor(tier: Tier): Color = when (tier) {
    Tier.BRONZE -> Knurl.colors.bronze
    Tier.SILVER -> Knurl.colors.silver
    Tier.GOLD -> Knurl.colors.gold
}

/** What a medal shows: the strength milestones carry the plates, everything else a plain symbol. */
private fun AchievementKind.icon(): ImageVector? = when (this) {
    AchievementKind.WORKOUTS -> Icons.Rounded.FitnessCenter
    AchievementKind.STREAK -> Icons.Rounded.LocalFireDepartment
    AchievementKind.SESSION_VOLUME -> Icons.Rounded.Bolt
    AchievementKind.TOTAL_VOLUME -> Icons.Rounded.Landscape
    AchievementKind.BENCH, AchievementKind.SQUAT, AchievementKind.DEADLIFT -> null
    AchievementKind.RECORDS -> Icons.Rounded.EmojiEvents
    AchievementKind.SUPERSET -> Icons.Rounded.Link
    AchievementKind.EARLY_BIRD -> Icons.Rounded.WbSunny
    AchievementKind.HYROX -> Icons.AutoMirrored.Rounded.DirectionsRun
    AchievementKind.WEIGH_INS -> Icons.Rounded.MonitorWeight
}

/**
 * A medal: a ring in the tier's metal around the symbol. Locked medals are grey, with the progress
 * so far drawn as an arc in the accent.
 */
@Composable
fun Medal(kind: AchievementKind, tier: Tier, unlocked: Boolean, progress: Float, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val metal = tierColor(tier)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val content = if (unlocked) metal else MaterialTheme.colorScheme.outline
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.08f
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(if (unlocked) metal else track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (!unlocked && progress > 0f) {
                drawArc(accent.copy(alpha = 0.55f), -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke * 0.7f, cap = StrokeCap.Round))
            }
        }
        Box(
            Modifier.padding(size * 0.14f).fillMaxSize().background(if (unlocked) metal.copy(alpha = 0.16f) else track.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icon = kind.icon()
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.38f))
            else KnurlMark(Modifier.size(size * 0.42f), color = content)
        }
    }
}
