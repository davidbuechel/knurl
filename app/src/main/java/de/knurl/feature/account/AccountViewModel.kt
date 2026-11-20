package de.knurl.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.core.export.DataExport
import de.knurl.core.notifications.PushRegistration
import de.knurl.data.firebase.AccountError
import de.knurl.data.firebase.AccountResult
import de.knurl.data.firebase.AuthRepository
import de.knurl.data.firebase.AuthState
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What happened last, shown once as a message. */
enum class AccountNotice { LINKED, SIGNED_IN, SIGNED_OUT, RESET_SENT, DELETED, EXPORTED, GOOGLE_FAILED }

data class AccountUiState(
    val anonymous: Boolean,
    val email: String?,
    val workouts: Int,
    val busy: Boolean,
    val error: AccountError?,
    val notice: AccountNotice?,
)

class AccountViewModel(
    private val auth: AuthRepository,
    private val training: TrainingRepository,
    private val push: PushRegistration,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<AccountError?>(null)
    private val _notice = MutableStateFlow<AccountNotice?>(null)

    val state: StateFlow<AccountUiState> = combine(auth.state, training.data, busy, error, _notice) { authState, data, busy, error, notice ->
        AccountUiState(
            anonymous = (authState as? AuthState.SignedIn)?.anonymous ?: true,
            email = auth.email(),
            workouts = data?.workouts?.size ?: 0,
            busy = busy,
            error = error,
            notice = notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState(true, null, 0, false, null, null))

    fun linkEmail(email: String, password: String) = run(AccountNotice.LINKED) { auth.linkEmail(email, password) }

    fun signInEmail(email: String, password: String) = run(AccountNotice.SIGNED_IN) { auth.signInEmail(email, password) }

    fun google(idToken: String, link: Boolean) =
        run(if (link) AccountNotice.LINKED else AccountNotice.SIGNED_IN) { if (link) auth.linkGoogle(idToken) else auth.signInGoogle(idToken) }

    fun googleFailed() {
        _notice.value = AccountNotice.GOOGLE_FAILED
    }

    fun sendReset(email: String) = run(AccountNotice.RESET_SENT) { auth.sendPasswordReset(email) }

    fun signOut() {
        viewModelScope.launch {
            push.unregister()
            auth.signOut()
            _notice.value = AccountNotice.SIGNED_OUT
        }
    }

    /** Deletes the data first (it needs the account to pass the rules), then the sign-in. */
    fun deleteAccount() = run(AccountNotice.DELETED) {
        if (training.deleteAllData()) auth.deleteUser() else AccountError.NETWORK
    }

    fun exportJson(): String? = training.data.value?.let(DataExport::json)

    fun exportCsv(): String? = training.data.value?.let { DataExport.csv(it, training::exercise) }

    fun exported() {
        _notice.value = AccountNotice.EXPORTED
    }

    fun clearMessages() {
        error.value = null
        _notice.value = null
    }

    private fun run(success: AccountNotice, action: suspend () -> AccountResult) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            val result = action()
            busy.value = false
            if (result == null) _notice.value = success else error.value = result
        }
    }
}
