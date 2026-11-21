package de.knurl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import de.knurl.domain.search.ExerciseFilter
import de.knurl.domain.calc.Period
import de.knurl.core.notifications.Notifications
import de.knurl.core.notifications.PushRegistration
import de.knurl.core.notifications.ReminderScheduler
import de.knurl.core.notifications.WorkoutNotifier
import de.knurl.core.settings.AppPreferences
import de.knurl.core.workout.RestTimer
import de.knurl.data.firebase.AuthRepository
import de.knurl.data.firebase.FirestoreTrainingStore
import de.knurl.core.health.HealthSync
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import de.knurl.domain.catalog.ExerciseCatalog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class KnurlApp : Application() {
    lateinit var container: AppContainer
        private set

    /** True while any screen of the app is visible; background-only notifications check it. */
    @Volatile
    var inForeground = false
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        container = AppContainer(this)
        container.preferences.applyTheme()
        registerActivityLifecycleCallbacks(ForegroundTracker())
    }

    private inner class ForegroundTracker : ActivityLifecycleCallbacks {
        private var started = 0
        override fun onActivityStarted(activity: Activity) { started++; inForeground = true }
        override fun onActivityStopped(activity: Activity) { started--; inForeground = started > 0 }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        // Health Connect permissions change in system settings: look again whenever the app comes back.
        override fun onActivityResumed(activity: Activity) = container.health.refresh()
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

/** Builds the app's long-lived objects once. Screens reach them through their ViewModels. */
class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob())

    val preferences = AppPreferences(app)

    private val firestore = FirebaseFirestore.getInstance()

    val auth = AuthRepository(FirebaseAuth.getInstance(), scope)

    val training = TrainingRepository(
        // Firestore keeps a persistent local cache by default: reads work offline, writes are queued.
        store = FirestoreTrainingStore(firestore),
        uid = auth.uid,
        scope = scope,
        german = { Locale.getDefault().language == "de" },
    )

    val restTimer = RestTimer(app)

    /**
     * The exercise picker's filter, kept between visits: building a chest routine means picking
     * chest again and again, and the filter should still be set when the picker opens next time.
     */
    val pickerFilter = MutableStateFlow(ExerciseFilter())

    /** Statistics' week/month/year, kept between visits. */
    val statsPeriod = MutableStateFlow(Period.WEEK)

    private val _catalogReady = MutableStateFlow(false)
    /** True once the full exercise catalog is read from assets; the core list works before that. */
    val catalogReady: StateFlow<Boolean> = _catalogReady.asStateFlow()

    val push = PushRegistration(auth, preferences, firestore, scope)

    val health = HealthSync(app, preferences, scope).also { training.listener = it; it.training = training }

    init {
        scope.launch(Dispatchers.IO) {
            runCatching { ExerciseCatalog.load(app.assets.open("exercises.json").bufferedReader().use { it.readText() }) }
                .onFailure { Log.e("Catalog", "Could not load exercises.json; using the core list", it) }
            _catalogReady.value = true
        }
        WorkoutNotifier(app, training, restTimer, scope)
        // Reminders follow the account's notification settings.
        scope.launch {
            training.data.filterNotNull().map { it.settings.notifications }.distinctUntilChanged()
                .collect { ReminderScheduler.schedule(app, it) }
        }
        // Crash reports only with consent (manifest default: off).
        scope.launch {
            preferences.crashReports.collect { FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = it }
        }
    }
}
