package de.knurl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.knurl.ui.navigation.KnurlNavHost
import de.knurl.ui.theme.KnurlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val training = (application as KnurlApp).container.training
        // Keep the splash up for the few milliseconds it takes to read the training file.
        splash.setKeepOnScreenCondition { training.data.value == null }
        enableEdgeToEdge()
        setContent {
            KnurlTheme {
                KnurlNavHost()
            }
        }
    }
}
