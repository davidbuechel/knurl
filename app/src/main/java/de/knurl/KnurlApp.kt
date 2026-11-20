package de.knurl

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.knurl.core.settings.AppPreferences
import de.knurl.data.firebase.AuthRepository
import de.knurl.data.firebase.FirestoreTrainingStore
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.Locale

class KnurlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.preferences.applyTheme()
    }
}

/** Builds the app's long-lived objects once. Screens reach them through their ViewModels. */
class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob())

    val preferences = AppPreferences(app)

    val auth = AuthRepository(FirebaseAuth.getInstance(), scope)

    val training = TrainingRepository(
        // Firestore keeps a persistent local cache by default: reads work offline, writes are queued.
        store = FirestoreTrainingStore(FirebaseFirestore.getInstance()),
        uid = auth.uid,
        scope = scope,
        german = { Locale.getDefault().language == "de" },
    )
}
