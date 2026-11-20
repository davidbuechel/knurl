package de.knurl

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.core.notifications.Notifications
import de.knurl.core.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import de.knurl.data.firebase.AuthState
import de.knurl.ui.brand.KnurlAppIcon
import de.knurl.ui.navigation.KnurlNavHost
import de.knurl.ui.theme.KnurlTheme

/** AppCompatActivity, so the in-app language and night mode apply (core/settings/AppPreferences). */
class MainActivity : AppCompatActivity() {

    /** Where a tapped notification wants to go; the nav host consumes it. */
    private val target = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        target.value = intent.getStringExtra(Notifications.EXTRA_TARGET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) target.value = intent.getStringExtra(Notifications.EXTRA_TARGET)
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as KnurlApp).container
        // Keep the splash up until the account is ready and the first data is read (cache: milliseconds).
        splash.setKeepOnScreenCondition {
            container.auth.state.value != AuthState.Offline && (container.training.data.value == null || !container.catalogReady.value)
        }
        enableEdgeToEdge()
        setContent {
            val theme by container.preferences.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            KnurlTheme(darkTheme = dark) {
                val auth by container.auth.state.collectAsStateWithLifecycle()
                if (auth == AuthState.Offline) FirstStartOffline(onRetry = container.auth::ensureSignedIn)
                else {
                    val pending by target.collectAsStateWithLifecycle()
                    KnurlNavHost(pendingTarget = pending, onTargetHandled = { target.value = null })
                }
            }
        }
    }
}

/** Only on the very first start: the anonymous account needs the network once. */
@Composable
private fun FirstStartOffline(onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KnurlAppIcon(Modifier.size(64.dp))
            Text(stringResource(R.string.first_start_offline_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.first_start_offline_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}
