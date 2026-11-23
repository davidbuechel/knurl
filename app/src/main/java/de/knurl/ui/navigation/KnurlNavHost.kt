package de.knurl.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
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
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.knurl.feature.achievements.AchievementsScreen
import de.knurl.feature.nutrition.AddFoodScreen
import de.knurl.feature.nutrition.NutritionGoalScreen
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
/** [added]: ids already in the workout or routine, comma-separated, marked in the list. */
@Serializable data class PickExerciseRoute(val added: String = "")
@Serializable data class ExerciseRoute(val id: String, val pick: Boolean = false)
@Serializable data class CustomExerciseRoute(val id: String? = null, val name: String = "", val pick: Boolean = false)
@Serializable data object SettingsRoute
@Serializable data object BodyWeightRoute
@Serializable data object CalendarRoute
@Serializable data object AccountRoute
@Serializable data object AchievementsRoute
/** [day]: ISO date the food goes to; [scan] opens the barcode scanner right away. */
@Serializable data class AddFoodRoute(val day: String, val scan: Boolean = false)
@Serializable data object NutritionGoalRoute
@Serializable data class RecapRoute(val year: Int, val month: Int)
@Serializable data class LegalRoute(val page: LegalPage)

/**
 * The only way screens move: `go.to(route)` and `go.back()`. Each destination gets its own, and it
 * only acts while its screen is the settled one on top: a second tap during a transition opens
 * nothing twice and pops nothing twice, and Home is never popped.
 */
class Navigator(private val nav: NavHostController, private val entry: NavBackStackEntry? = null) {
    private fun ready() = entry == null || entry.lifecycle.currentState == Lifecycle.State.RESUMED

    fun <T : Any> to(route: T) {
        if (ready()) nav.navigate(route) { launchSingleTop = true }
    }

    /** Goes back [times] screens at once (never past Home). */
    fun back(times: Int = 1) {
        if (!ready()) return
        repeat(times) { if (nav.previousBackStackEntry != null) nav.popBackStack() }
    }

    /** Goes back and hands the previous screen a value (e.g. the exercise picked). */
    fun backWithResult(key: String, value: String) {
        if (!ready()) return
        nav.previousBackStackEntry?.savedStateHandle?.set(key, value)
        nav.popBackStack()
    }

    /** Goes back and hands the previous screen several values (the exercises picked, in order). */
    fun backWithResults(key: String, values: List<String>) {
        if (!ready()) return
        nav.previousBackStackEntry?.savedStateHandle?.set(key, ArrayList(values))
        nav.popBackStack()
    }

    /** Replaces the current screen, so back skips it (workout → summary). */
    fun <T : Any> replace(route: T) {
        if (!ready()) return
        nav.navigate(route) { nav.currentDestination?.id?.let { popUpTo(it) { inclusive = true } } }
    }

    companion object {
        /** One exercise: an own exercise just created, handed back to the picker. */
        const val RESULT_EXERCISE = "pickedExercise"
        /** The exercises picked, in the order they were tapped, handed to the workout or routine. */
        const val RESULT_EXERCISES = "pickedExercises"
    }
}

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
        if (current != null && training.needsOnboarding(current)) OnboardingScreen() else Destinations(nav)
    }
}

@Composable
private fun Destinations(nav: NavHostController) {
    // The screen's width: the back gesture's transitions move by parts of it.
    val width = LocalWindowInfo.current.containerSize.width
    // Black behind everything: the screen below a moving one darkens against it (ScreenMotion).
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            enterTransition = { ScreenMotion.enter() },
            exitTransition = { ScreenMotion.exit() },
            popEnterTransition = { ScreenMotion.popEnter() },
            popExitTransition = { ScreenMotion.popExit() },
            predictivePopEnterTransition = { edge -> ScreenMotion.gestureEnter(edge, width) },
            predictivePopExitTransition = { edge -> ScreenMotion.gestureExit(edge, width) },
        ) {
            screen<HomeRoute>(nav) { go, _ -> HomeScreen(go) }
            screen<ActiveWorkoutRoute>(nav) { go, entry -> ActiveWorkoutScreen(go, entry.savedStateHandle) }
            screen<EditWorkoutRoute>(nav) { go, entry -> ActiveWorkoutScreen(go, entry.savedStateHandle, editWorkoutId = entry.toRoute<EditWorkoutRoute>().id) }
            screen<SummaryRoute>(nav) { go, entry -> SummaryScreen(go, entry.toRoute<SummaryRoute>().workoutId) }
            screen<RoutineEditorRoute>(nav) { go, entry -> RoutineEditorScreen(go, entry.toRoute<RoutineEditorRoute>().id, entry.savedStateHandle) }
            screen<PickExerciseRoute>(nav) { go, entry ->
                val added = entry.toRoute<PickExerciseRoute>().added.split(',').filter { it.isNotBlank() }.toSet()
                ExercisesScreen(go, pickForWorkout = true, savedState = entry.savedStateHandle, alreadyAdded = added)
            }
            screen<ExerciseRoute>(nav) { go, entry -> entry.toRoute<ExerciseRoute>().let { ExerciseDetailScreen(go, it.id, pickForWorkout = it.pick) } }
            screen<CustomExerciseRoute>(nav) { go, entry ->
                val route = entry.toRoute<CustomExerciseRoute>()
                CustomExerciseScreen(go, route.id, route.name, pickForWorkout = route.pick)
            }
            screen<SettingsRoute>(nav) { go, _ -> SettingsScreen(go) }
            screen<BodyWeightRoute>(nav) { go, _ -> BodyWeightScreen(go) }
            screen<CalendarRoute>(nav) { go, _ -> CalendarScreen(go) }
            screen<AccountRoute>(nav) { go, _ -> AccountScreen(go) }
            screen<AchievementsRoute>(nav) { go, _ -> AchievementsScreen(go) }
            screen<AddFoodRoute>(nav) { go, entry -> entry.toRoute<AddFoodRoute>().let { AddFoodScreen(go, it.day, it.scan) } }
            screen<NutritionGoalRoute>(nav) { go, _ -> NutritionGoalScreen(go) }
            screen<RecapRoute>(nav) { go, entry -> entry.toRoute<RecapRoute>().let { MonthRecapScreen(go, it.year, it.month) } }
            screen<LegalRoute>(nav) { go, entry -> LegalScreen(go, entry.toRoute<LegalRoute>().page) }
        }
    }
}
