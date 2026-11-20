package de.knurl.data.firebase

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    data object SigningIn : AuthState
    data class SignedIn(val uid: String, val anonymous: Boolean) : AuthState
    /** The very first start needs the network once to create the anonymous account. */
    data object Offline : AuthState
}

enum class AccountError { INVALID_EMAIL, WEAK_PASSWORD, WRONG_CREDENTIALS, EMAIL_IN_USE, NETWORK, REAUTH_NEEDED, UNKNOWN }

/** Null on success. */
typealias AccountResult = AccountError?

/**
 * Everyone has an account from the first start: an anonymous one. Linking it to Google or e-mail
 * keeps the uid, so all data stays where it is. Signing in to an existing account switches to that
 * account's data; signing out starts a fresh anonymous account.
 */
class AuthRepository(private val auth: FirebaseAuth, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<AuthState>(AuthState.SigningIn)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    init {
        auth.addAuthStateListener { firebase ->
            val user = firebase.currentUser
            _uid.value = user?.uid
            if (user != null) _state.value = AuthState.SignedIn(user.uid, user.isAnonymous)
        }
        ensureSignedIn()
    }

    fun email(): String? = auth.currentUser?.email

    fun ensureSignedIn() {
        if (auth.currentUser != null) return
        _state.value = AuthState.SigningIn
        scope.launch {
            runCatching { auth.signInAnonymously().await() }
                .onFailure {
                    Log.w(TAG, "Anonymous sign-in failed", it)
                    _state.value = AuthState.Offline
                }
        }
    }

    /** Keeps the anonymous account's data and adds e-mail and password to it. */
    suspend fun linkEmail(email: String, password: String): AccountResult =
        link(EmailAuthProvider.getCredential(email.trim(), password))

    /** Keeps the anonymous account's data and adds the Google account to it. */
    suspend fun linkGoogle(idToken: String): AccountResult = link(GoogleAuthProvider.getCredential(idToken, null))

    suspend fun signInEmail(email: String, password: String): AccountResult =
        attempt { auth.signInWithEmailAndPassword(email.trim(), password).await() }

    suspend fun signInGoogle(idToken: String): AccountResult =
        attempt { auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await() }

    suspend fun sendPasswordReset(email: String): AccountResult =
        attempt { auth.sendPasswordResetEmail(email.trim()).await() }

    /** Signs out and starts a fresh anonymous account at once, so the app always has one. */
    fun signOut() {
        auth.signOut()
        ensureSignedIn()
    }

    /** Deletes the sign-in itself. Call after the data is gone; then a fresh anonymous account starts. */
    suspend fun deleteUser(): AccountResult {
        val user = auth.currentUser ?: return null
        return attempt { user.delete().await() }.also { if (it == null) ensureSignedIn() }
    }

    private suspend fun link(credential: AuthCredential): AccountResult {
        val user = auth.currentUser ?: return AccountError.UNKNOWN
        return attempt {
            user.linkWithCredential(credential).await()
            // isAnonymous changes without a new auth state event; publish it.
            _state.value = AuthState.SignedIn(user.uid, anonymous = false)
        }
    }

    private suspend fun attempt(block: suspend () -> Unit): AccountResult = try {
        block()
        null
    } catch (e: Exception) {
        Log.w(TAG, "Account action failed", e)
        e.toAccountError()
    }

    private fun Exception.toAccountError(): AccountError = when (this) {
        is FirebaseNetworkException -> AccountError.NETWORK
        is FirebaseAuthWeakPasswordException -> AccountError.WEAK_PASSWORD
        is FirebaseAuthUserCollisionException -> AccountError.EMAIL_IN_USE
        is FirebaseAuthRecentLoginRequiredException -> AccountError.REAUTH_NEEDED
        is FirebaseAuthInvalidUserException -> AccountError.WRONG_CREDENTIALS
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == "ERROR_INVALID_EMAIL") AccountError.INVALID_EMAIL else AccountError.WRONG_CREDENTIALS
        else -> AccountError.UNKNOWN
    }

    private companion object {
        const val TAG = "Auth"
    }
}
