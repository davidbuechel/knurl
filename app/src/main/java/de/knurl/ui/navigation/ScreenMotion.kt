package de.knurl.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * How screens move, set once in [KnurlNavHost]; screens add no transitions of their own.
 *
 * A stack of cards: a new screen slides in from the right over the current one, which drifts a
 * little to the left and darkens; back is the same in reverse. Nothing is ever half transparent on
 * top of something else, so two screens never show through each other.
 *
 * The back gesture (predictive back) is driven by the finger: NavHost seeks the gesture
 * transitions by its progress. The top screen shrinks a little, rounds its corners and follows the
 * finger while the one below comes forward; letting go glides it off, cancelling springs it back.
 * Left unset, Navigation's default shrinks the screen to 70 % and then cuts it away.
 */
internal object ScreenMotion {
    private const val PUSH_MS = 400
    private const val POP_MS = 340
    private const val GESTURE_MS = 400
    /** Share of the gesture that only shrinks and nudges; the rest slides off. */
    private const val GESTURE_HOLD = 0.4f

    /** How far the screen below moves, as a share of the width. */
    private const val PARALLAX = 0.25f
    /** How visible the screen below is over the black backdrop. */
    private const val DIM = 0.6f
    private const val GESTURE_SCALE = 0.9f
    private const val EDGE_RIGHT = 1 // BackEventCompat.EDGE_RIGHT

    /** Material's emphasized curve: full-width moves start softly and settle long. */
    private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val Glide = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    private val GestureCorner = 28.dp

    fun enter(): EnterTransition = slideInHorizontally(tween(PUSH_MS, easing = Emphasized)) { it }

    fun exit(): ExitTransition =
        slideOutHorizontally(tween(PUSH_MS, easing = Emphasized)) { -(it * PARALLAX).toInt() } +
            fadeOut(tween(PUSH_MS, easing = Emphasized), targetAlpha = DIM)

    fun popEnter(): EnterTransition =
        slideInHorizontally(tween(POP_MS, easing = Emphasized)) { -(it * PARALLAX).toInt() } +
            fadeIn(tween(POP_MS, easing = Emphasized), initialAlpha = DIM)

    fun popExit(): ExitTransition = slideOutHorizontally(tween(POP_MS, easing = Emphasized)) { it }

    /** The screen below during the gesture: comes in from slightly to the side and brightens. */
    fun gestureEnter(edge: Int, width: Int): EnterTransition {
        val dir = if (edge == EDGE_RIGHT) -1 else 1
        return slideInHorizontally(tween(GESTURE_MS, easing = LinearEasing)) { -dir * (width * PARALLAX / 2).toInt() } +
            fadeIn(tween(GESTURE_MS, easing = LinearEasing), initialAlpha = DIM)
    }

    /** The top screen during the gesture: shrinks and follows the finger, then slides off. */
    fun gestureExit(edge: Int, width: Int): ExitTransition {
        val dir = if (edge == EDGE_RIGHT) -1 else 1
        val hold = (GESTURE_MS * GESTURE_HOLD).toInt()
        val slide = keyframes {
            durationMillis = GESTURE_MS
            IntOffset.Zero at 0 using LinearEasing
            IntOffset(dir * width / 16, 0) at hold using Glide
        }
        return slideOutHorizontally(slide) { dir * it } + scaleOut(tween(hold, easing = LinearEasing), targetScale = GESTURE_SCALE)
    }

    /** Corner radius of a leaving screen, in step with its transition (also while a gesture seeks it). */
    @Composable
    fun AnimatedContentScope.leavingCorner(): Dp {
        val corner by transition.animateDp(
            transitionSpec = { tween((GESTURE_MS * GESTURE_HOLD).toInt(), easing = LinearEasing) },
            label = "corner",
        ) { state -> if (state == EnterExitState.PostExit) GestureCorner else 0.dp }
        return corner
    }
}

/**
 * A destination with the app's motion: like `composable`, opaque, with rounded corners while it
 * leaves, and its own [Navigator] that only acts while this screen is the settled one on top.
 */
internal inline fun <reified T : Any> NavGraphBuilder.screen(
    nav: NavHostController,
    noinline content: @Composable AnimatedContentScope.(go: Navigator, entry: NavBackStackEntry) -> Unit,
) = composable<T> { entry ->
    val go = remember(entry) { Navigator(nav, entry) }
    val corner = with(ScreenMotion) { leavingCorner() }
    // Opaque on its own: the black backdrop behind the NavHost only ever shows around a shrinking screen.
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.background)) { content(go, entry) }
}
