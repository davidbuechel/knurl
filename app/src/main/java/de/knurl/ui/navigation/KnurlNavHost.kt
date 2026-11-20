package de.knurl.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import de.knurl.core.notifications.Notifications
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.data.WeightUnit
import de.knurl.feature.account.AccountScreen
import de.knurl.feature.onboarding.OnboardingScreen
import de.knurl.feature.routines.RoutineEditorScreen
import de.knurl.feature.summary.SummaryScreen
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.feature.common.appContainer
import de.knurl.feature.legal.LegalPage
import de.knurl.feature.legal.LegalScreen
import de.knurl.feature.settings.SettingsScreen
import de.knurl.feature.progress.BodyWeightScreen
import de.knurl.feature.progress.CalendarScreen
import de.knurl.feature.progress.ProgressScreen
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.knurl.feature.achievements.AchievementsScreen
import de.knurl.feature.exercises.CustomExerciseScreen
import de.knurl.feature.recap.MonthRecapScreen
import de.knurl.feature.exercises.ExerciseDetailScreen
import de.knurl.feature.exercises.ExercisesScreen
import de.knurl.feature.home.HomeScreen
import de.knurl.feature.workout.ActiveWorkoutScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ActiveWorkoutRoute
@Serializable data class EditWorkoutRoute(val id: String)
@Serializable data class SummaryRoute(val workoutId: String)
@Serializable data class RoutineEditorRoute(val id: String? = null)
@Serializable data object PickExerciseRoute
@Serializable data class ExerciseRoute(val id: String)
@Serializable data class CustomExerciseRoute(val id: String? = null, val name: String = "", val pick: Boolean = false)
@Serializable data object SettingsRoute
@Serializable data object StatsRoute
@Serializable data object BodyWeightRoute
@Serializable data object CalendarRoute
@Serializable data object AccountRoute
@Serializable data object AchievementsRoute
@Serializable data class RecapRoute(val year: Int, val month: Int)
@Serializable data class LegalRoute(val page: LegalPage)

/** The only way screens move: `go.to(route)` and `go.back()`. */
class Navigator(private val nav: NavHostController) {
    fun <T : Any> to(route: T) = nav.navigate(route) { launchSingleTop = true }
    fun back() {
        nav.popBackStack()
    }

    /** Goes back and hands the previous screen a value (e.g. the exercise picked). */
    fun backWithResult(key: String, value: String) {
        nav.previousBackStackEntry?.savedStateHandle?.set(key, value)
        nav.popBackStack()
    }

    /** Replaces the current screen, so back skips it (workout → summary). */
    fun <T : Any> replace(route: T) = nav.navigate(route) {
        nav.currentDestination?.id?.let { popUpTo(it) { inclusive = true } }
    }

    companion object {
        const val RESULT_EXERCISE = "pickedExercise"
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
fun KnurlNavHost(pendingTarget: String? = null, onTargetHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val go = remember(nav) { Navigator(nav) }
    // A tapped notification: the workout notifications lead to the running workout.
    LaunchedEffect(pendingTarget) {
        if (pendingTarget == Notifications.TARGET_WORKOUT) go.to(ActiveWorkoutRoute)
        if (pendingTarget != null) onTargetHandled()
    }
    val training = appContainer().training
    val data by training.data.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalWeightUnit provides (data?.settings?.unit ?: WeightUnit.KG)) {
        // A new account answers three questions first (OnboardingScreen); everyone else goes straight in.
        val current = data
        if (current != null && training.needsOnboarding(current)) OnboardingScreen() else Destinations(nav, go)
    }
}

@Composable
private fun Destinations(nav: NavHostController, go: Navigator) {
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        enterTransition = ScreenMotion.enter,
        exitTransition = ScreenMotion.exit,
        popEnterTransition = ScreenMotion.popEnter,
        popExitTransition = ScreenMotion.popExit,
    ) {
        composable<HomeRoute> { HomeScreen(go) }
        composable<ActiveWorkoutRoute> { entry -> ActiveWorkoutScreen(go, entry.savedStateHandle) }
        composable<EditWorkoutRoute> { entry -> ActiveWorkoutScreen(go, entry.savedStateHandle, editWorkoutId = entry.toRoute<EditWorkoutRoute>().id) }
        composable<SummaryRoute> { entry -> SummaryScreen(go, entry.toRoute<SummaryRoute>().workoutId) }
        composable<RoutineEditorRoute> { entry -> RoutineEditorScreen(go, entry.toRoute<RoutineEditorRoute>().id, entry.savedStateHandle) }
        composable<PickExerciseRoute> { entry -> ExercisesScreen(go, pickForWorkout = true, savedState = entry.savedStateHandle) }
        composable<ExerciseRoute> { entry -> ExerciseDetailScreen(go, entry.toRoute<ExerciseRoute>().id) }
        composable<CustomExerciseRoute> { entry ->
            val route = entry.toRoute<CustomExerciseRoute>()
            CustomExerciseScreen(go, route.id, route.name, pickForWorkout = route.pick)
        }
        composable<SettingsRoute> { SettingsScreen(go) }
        composable<StatsRoute> { ProgressScreen(go) }
        composable<BodyWeightRoute> { BodyWeightScreen(go) }
        composable<CalendarRoute> { CalendarScreen(go) }
        composable<AccountRoute> { AccountScreen(go) }
        composable<AchievementsRoute> { AchievementsScreen(go) }
        composable<RecapRoute> { entry -> entry.toRoute<RecapRoute>().let { MonthRecapScreen(go, it.year, it.month) } }
        composable<LegalRoute> { entry -> LegalScreen(go, entry.toRoute<LegalRoute>().page) }
    }
}
