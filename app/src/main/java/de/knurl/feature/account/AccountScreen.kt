package de.knurl.feature.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.data.firebase.AccountError
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.settings.LinkRow
import de.knurl.feature.settings.Segmented
import de.knurl.feature.settings.SettingsSection
import de.knurl.ui.navigation.Navigator
import kotlinx.coroutines.launch

private enum class Mode { SECURE, SIGN_IN }
private enum class Confirm { SIGN_IN_ELSEWHERE, SIGN_OUT, DELETE }

/**
 * Securing the anonymous account (Google or e-mail), signing in to an existing one, signing out,
 * exporting all data and deleting the account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(go: Navigator) {
    val vm = knurlViewModel { AccountViewModel(it.auth, it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var mode by rememberSaveable { mutableStateOf(Mode.SECURE) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }
    var pendingGoogleSignIn by rememberSaveable { mutableStateOf(false) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = vm.exportJson()
        if (uri != null && text != null) context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }.also { vm.exported() }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val text = vm.exportCsv()
        if (uri != null && text != null) context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }.also { vm.exported() }
    }

    fun google(link: Boolean) = scope.launch {
        when (val result = GoogleSignIn.idToken(context)) {
            is GoogleSignIn.Result.Token -> vm.google(result.idToken, link)
            GoogleSignIn.Result.Cancelled -> Unit
            GoogleSignIn.Result.Failed -> vm.googleFailed()
        }
    }

    val messageText = state.error?.let { stringResource(it.message()) } ?: state.notice?.let { stringResource(it.message()) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            vm.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.anonymous) {
                item {
                    SettingsSection(stringResource(R.string.account_secure_title)) {
                        Segmented(Mode.entries, mode, { stringResource(if (it == Mode.SECURE) R.string.account_mode_secure else R.string.account_mode_sign_in) }) { mode = it }
                        Text(
                            stringResource(if (mode == Mode.SECURE) R.string.account_secure_body else R.string.account_sign_in_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                if (mode == Mode.SECURE) google(link = true)
                                else if (state.workouts > 0) { pendingGoogleSignIn = true; confirm = Confirm.SIGN_IN_ELSEWHERE }
                                else google(link = false)
                            },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.account_google)) }
                        Text(stringResource(R.string.account_or_email), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = email, onValueChange = { email = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.account_email)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.account_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        OutlinedButton(
                            onClick = {
                                if (mode == Mode.SECURE) vm.linkEmail(email, password)
                                else if (state.workouts > 0) { pendingGoogleSignIn = false; confirm = Confirm.SIGN_IN_ELSEWHERE }
                                else vm.signInEmail(email, password)
                            },
                            enabled = !state.busy && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(if (mode == Mode.SECURE) R.string.account_create else R.string.account_sign_in)) }
                        if (mode == Mode.SIGN_IN) {
                            TextButton(onClick = { vm.sendReset(email) }, enabled = email.isNotBlank()) { Text(stringResource(R.string.account_forgot)) }
                        }
                    }
                }
            } else {
                item {
                    SettingsSection(stringResource(R.string.account_signed_in_as)) {
                        Text(state.email ?: stringResource(R.string.account_linked), style = MaterialTheme.typography.titleMedium)
                        state.email?.let { address ->
                            LinkRow(Icons.Outlined.Key, stringResource(R.string.account_change_password), stringResource(R.string.account_change_password_hint)) { vm.sendReset(address) }
                        }
                        LinkRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.account_sign_out)) { confirm = Confirm.SIGN_OUT }
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.account_your_data)) {
                    LinkRow(Icons.Outlined.DataObject, stringResource(R.string.account_export_json), stringResource(R.string.account_export_json_hint)) { exportJson.launch("knurl-export.json") }
                    LinkRow(Icons.Outlined.TableChart, stringResource(R.string.account_export_csv), stringResource(R.string.account_export_csv_hint)) { exportCsv.launch("knurl-sets.csv") }
                    LinkRow(Icons.Outlined.DeleteForever, stringResource(R.string.account_delete), stringResource(R.string.account_delete_hint), tint = MaterialTheme.colorScheme.error) { confirm = Confirm.DELETE }
                }
            }
        }
    }

    when (confirm) {
        Confirm.SIGN_IN_ELSEWHERE -> ConfirmDialog(
            title = stringResource(R.string.account_switch_title),
            body = stringResource(R.string.account_switch_body, state.workouts),
            confirm = stringResource(R.string.account_sign_in),
            onDismiss = { confirm = null },
            onConfirm = {
                confirm = null
                if (pendingGoogleSignIn) google(link = false) else vm.signInEmail(email, password)
            },
        )
        Confirm.SIGN_OUT -> ConfirmDialog(
            title = stringResource(R.string.account_sign_out_title),
            body = stringResource(R.string.account_sign_out_body),
            confirm = stringResource(R.string.account_sign_out),
            onDismiss = { confirm = null },
            onConfirm = { confirm = null; vm.signOut() },
        )
        Confirm.DELETE -> ConfirmDialog(
            title = stringResource(R.string.account_delete_title),
            body = stringResource(R.string.account_delete_body),
            confirm = stringResource(R.string.account_delete),
            onDismiss = { confirm = null },
            onConfirm = { confirm = null; vm.deleteAccount() },
        )
        null -> Unit
    }
}

private fun AccountError.message(): Int = when (this) {
    AccountError.INVALID_EMAIL -> R.string.account_error_email
    AccountError.WEAK_PASSWORD -> R.string.account_error_weak
    AccountError.WRONG_CREDENTIALS -> R.string.account_error_wrong
    AccountError.EMAIL_IN_USE -> R.string.account_error_in_use
    AccountError.NETWORK -> R.string.account_error_network
    AccountError.REAUTH_NEEDED -> R.string.account_error_reauth
    AccountError.UNKNOWN -> R.string.account_error_unknown
}

private fun AccountNotice.message(): Int = when (this) {
    AccountNotice.LINKED -> R.string.account_notice_linked
    AccountNotice.SIGNED_IN -> R.string.account_notice_signed_in
    AccountNotice.SIGNED_OUT -> R.string.account_notice_signed_out
    AccountNotice.RESET_SENT -> R.string.account_notice_reset
    AccountNotice.DELETED -> R.string.account_notice_deleted
    AccountNotice.EXPORTED -> R.string.account_notice_exported
    AccountNotice.GOOGLE_FAILED -> R.string.account_notice_google_failed
}
