package de.knurl.feature.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.knurl.R
import de.knurl.ui.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
enum class LegalPage(@StringRes val title: Int, @StringRes val body: Int) {
    IMPRINT(R.string.legal_imprint, R.string.legal_imprint_body),
    PRIVACY(R.string.legal_privacy, R.string.legal_privacy_body),
    LICENSES(R.string.legal_licenses, R.string.legal_licenses_body),
}

/** Placeholder texts until the real imprint and privacy policy exist (README: before release). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(go: Navigator, page: LegalPage) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(page.title)) },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
