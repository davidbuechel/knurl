package de.knurl.core.health

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.knurl.R
import de.knurl.ui.theme.KnurlTheme

/**
 * Health Connect opens this when people ask why Knurl wants health data (required by Health
 * Connect, ACTION_SHOW_PERMISSIONS_RATIONALE and VIEW_PERMISSION_USAGE in the manifest). It shows
 * the privacy policy, which has a section on Health Connect.
 */
class HealthPrivacyActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnurlTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.legal_privacy)) },
                            navigationIcon = { IconButton(onClick = ::finish) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                        )
                    },
                ) { padding ->
                    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)) {
                        item { Text(stringResource(R.string.legal_privacy_body), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }
    }
}
