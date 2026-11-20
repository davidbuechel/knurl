package de.knurl.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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

/**
 * Everyone has an account from the first start: an anonymous one, which can later be linked to
 * Google or e-mail without losing data (feature/account).
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

    private companion object {
        const val TAG = "Auth"
    }
}
