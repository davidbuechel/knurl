package de.knurl.feature.account

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import de.knurl.R

/**
 * Asks Android for a Google ID token (Credential Manager bottom sheet). The token is then handed to
 * Firebase. Null when the person cancels or no Google account is available.
 */
object GoogleSignIn {
    sealed interface Result {
        data class Token(val idToken: String) : Result
        data object Cancelled : Result
        data object Failed : Result
    }

    suspend fun idToken(context: Context): Result {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        return try {
            val response = CredentialManager.create(context)
                .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            Log.w("GoogleSignIn", "No Google account on this device", e)
            Result.Failed
        } catch (e: Exception) {
            Log.w("GoogleSignIn", "No Google credential", e)
            Result.Failed
        }
    }
}
