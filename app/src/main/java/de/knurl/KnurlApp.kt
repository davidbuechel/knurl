package de.knurl

import android.app.Application
import de.knurl.data.TrainingData
import de.knurl.data.local.JsonFileStore
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.Locale

class KnurlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Builds the app's long-lived objects once. Screens reach them through their ViewModels. */
class AppContainer(app: Application) {
    private val appScope = CoroutineScope(SupervisorJob())

    val training = TrainingRepository(
        store = JsonFileStore(File(app.filesDir, "training.json"), TrainingData.serializer()),
        scope = appScope,
        german = { Locale.getDefault().language == "de" },
    )
}
