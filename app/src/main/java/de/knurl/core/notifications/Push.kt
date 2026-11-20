package de.knurl.core.notifications

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.knurl.KnurlApp
import de.knurl.core.settings.AppPreferences
import de.knurl.data.firebase.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Keeps users/{uid}/devices/{installId} up to date with this phone's FCM token, so pushes for an
 * account reach every phone it is signed in on. Removed on sign-out.
 */
class PushRegistration(
    private val auth: AuthRepository,
    private val prefs: AppPreferences,
    private val db: FirebaseFirestore,
    private val scope: CoroutineScope,
) {
    init {
        scope.launch { auth.uid.collect { uid -> if (uid != null) register(uid, token = null) } }
    }

    fun onNewToken(token: String) {
        auth.uid.value?.let { uid -> scope.launch { register(uid, token) } }
    }

    /** Call before signing out: afterwards the rules no longer let us touch the old account. */
    suspend fun unregister() {
        val uid = auth.uid.value ?: return
        runCatching { device(uid).delete().await() }.onFailure { Log.w(TAG, "Could not remove device", it) }
    }

    private suspend fun register(uid: String, token: String?) {
        runCatching {
            val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
            device(uid).set(mapOf(
                "token" to fcmToken,
                "platform" to "android",
                "locale" to Locale.getDefault().toLanguageTag().take(16),
                "updatedAt" to FieldValue.serverTimestamp(),
            )).await()
        }.onFailure { Log.w(TAG, "Could not register for push", it) }
    }

    private fun device(uid: String) = db.collection("users").document(uid).collection("devices").document(prefs.installId)

    private companion object {
        const val TAG = "Push"
    }
}

/**
 * Shows pushes on the channel they name (data "channel"), if the person has that kind switched on.
 * Payload: data { channel, title, body } (docs/notifications.md).
 */
class KnurlMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        (application as KnurlApp).container.push.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val channel = Channel.byId(message.data["channel"]) ?: Channel.NEWS
        val settings = (application as KnurlApp).container.training.data.value?.settings?.notifications
        if (settings != null && !channel.enabledIn(settings)) return
        val title = message.data["title"] ?: message.notification?.title ?: return
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        Notifications.show(this, channel, id = 1000 + (message.messageId ?: title).hashCode() % 1000, title = title, body = body)
    }
}
