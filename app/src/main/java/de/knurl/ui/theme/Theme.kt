package de.knurl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Brand: steel neutrals, chalk surfaces, cobalt for action. Competition plate colours carry data.
private val Cobalt = Color(0xFF2448E8)
private val CobaltDark = Color(0xFF7C93FF)

private val LightColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E7FF),
    onPrimaryContainer = Color(0xFF0A1240),
    secondary = Color(0xFF3B4556),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E7EF),
    onSecondaryContainer = Color(0xFF151B26),
    tertiary = Color(0xFFD93A30),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD5),
    onTertiaryContainer = Color(0xFF410001),
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF11151C),
    surface = Color(0xFFF3F5F8),
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE8ECF1),
    onSurfaceVariant = Color(0xFF5A6473),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFEEF1F5),
    surfaceContainerHigh = Color(0xFFE8ECF1),
    surfaceContainerHighest = Color(0xFFE1E6EC),
    outline = Color(0xFF8A94A3),
    outlineVariant = Color(0xFFD5DBE3),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = CobaltDark,
    onPrimary = Color(0xFF0A1240),
    primaryContainer = Color(0xFF1D2550),
    onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFFB7C0CE),
    onSecondary = Color(0xFF212A37),
    secondaryContainer = Color(0xFF2A3240),
    onSecondaryContainer = Color(0xFFDDE3EC),
    tertiary = Color(0xFFFF6A5E),
    onTertiary = Color(0xFF410001),
    tertiaryContainer = Color(0xFF5C1712),
    onTertiaryContainer = Color(0xFFFFDAD5),
    background = Color(0xFF0C0F14),
    onBackground = Color(0xFFEEF1F5),
    surface = Color(0xFF0C0F14),
    onSurface = Color(0xFFEEF1F5),
    surfaceVariant = Color(0xFF1C222C),
    onSurfaceVariant = Color(0xFF9AA4B3),
    surfaceContainerLowest = Color(0xFF151A22),
    surfaceContainerLow = Color(0xFF12161D),
    surfaceContainer = Color(0xFF1C222C),
    surfaceContainerHigh = Color(0xFF232A35),
    surfaceContainerHighest = Color(0xFF2A3240),
    outline = Color(0xFF6B7584),
    outlineVariant = Color(0xFF2A3240),
    error = Color(0xFFFFB4AB),
)

/** Colours Material has no slot for: the plate palette and status colours. */
@Immutable
data class KnurlColors(
    val plate25: Color,
    val plate20: Color,
    val plate15: Color,
    val plate10: Color,
    val plate5: Color,
    val success: Color,
    val warning: Color,
) {
    /** Colour of a competition plate by its weight; small change plates are steel grey. */
    fun plate(kg: Double): Color = when {
        kg >= 25.0 -> plate25
        kg >= 20.0 -> plate20
        kg >= 15.0 -> plate15
        kg >= 10.0 -> plate10
        else -> plate5
    }
}

private val LightExtra = KnurlColors(
    plate25 = Color(0xFFD93A30), plate20 = Color(0xFF2B5BD7), plate15 = Color(0xFFE9B724),
    plate10 = Color(0xFF1E9455), plate5 = Color(0xFFC9CED6),
    success = Color(0xFF1E9455), warning = Color(0xFFC98A00),
)

private val DarkExtra = KnurlColors(
    plate25 = Color(0xFFFF6A5E), plate20 = Color(0xFF6D93FF), plate15 = Color(0xFFFFD055),
    plate10 = Color(0xFF3FCB83), plate5 = Color(0xFF8D96A3),
    success = Color(0xFF3FCB83), warning = Color(0xFFFFC24D),
)

val LocalKnurlColors = staticCompositionLocalOf { LightExtra }

private val KnurlShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KnurlTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKnurlColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = KnurlTypography,
            shapes = KnurlShapes,
            content = content,
        )
    }
}

object Knurl {
    val colors: KnurlColors @Composable get() = LocalKnurlColors.current
}
