package de.knurl.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.knurl.feature.exercises.ExerciseDetailScreen
import de.knurl.feature.exercises.ExercisesScreen
import de.knurl.feature.home.HomeScreen
import de.knurl.feature.workout.ActiveWorkoutScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ActiveWorkoutRoute
@Serializable data object PickExerciseRoute
@Serializable data class ExerciseRoute(val id: String)

/** The only way screens move: `go.to(route)` and `go.back()`. */
class Navigator(private val nav: NavHostController) {
    fun <T : Any> to(route: T) = nav.navigate(route) { launchSingleTop = true }
    fun back() {
        nav.popBackStack()
    }
}

/** Motion is defined once here: new screens slide over, back slides them away. Screens add no transitions. */
private object ScreenMotion {
    private const val MS = 320
    val enter: TransitionEnter = { slideIntoContainer(SlideDirection.Start, tween(MS)) }
    val exit: TransitionExit = { fadeOut(tween(MS)) }
    val popEnter: TransitionEnter = { fadeIn(tween(MS)) }
    val popExit: TransitionExit = { slideOutOfContainer(SlideDirection.End, tween(MS)) }
}

private typealias TransitionEnter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias TransitionExit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

@Composable
fun KnurlNavHost() {
    val nav = rememberNavController()
    val go = remember(nav) { Navigator(nav) }
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        enterTransition = ScreenMotion.enter,
        exitTransition = ScreenMotion.exit,
        popEnterTransition = ScreenMotion.popEnter,
        popExitTransition = ScreenMotion.popExit,
    ) {
        composable<HomeRoute> { HomeScreen(go) }
        composable<ActiveWorkoutRoute> { ActiveWorkoutScreen(go) }
        composable<PickExerciseRoute> { ExercisesScreen(go, pickForWorkout = true) }
        composable<ExerciseRoute> { entry -> ExerciseDetailScreen(go, entry.toRoute<ExerciseRoute>().id) }
    }
}
