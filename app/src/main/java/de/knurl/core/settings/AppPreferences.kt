package de.knurl.core.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Null tag = follow the system language. */
enum class AppLanguage(val tag: String?) { SYSTEM(null), GERMAN("de"), ENGLISH("en") }

/**
 * Settings that belong to this phone, not the account: how the app looks and behaves here.
 * Account-wide settings (units, plates, notifications) live in Firestore (data/Settings).
 * Language goes through AppCompat, so it also shows in Android's per-app language settings.
 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(enumOr(KEY_THEME, ThemeMode.SYSTEM))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _theme.value = mode
        applyTheme()
    }

    /** Tells Android too, so dialogs, system bars and the splash match the choice. */
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            when (_theme.value) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }

    val language: AppLanguage
        get() {
            val tag = AppCompatDelegate.getApplicationLocales().get(0)?.language
            return AppLanguage.entries.firstOrNull { it.tag != null && it.tag == tag } ?: AppLanguage.SYSTEM
        }

    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_ON, true))
    /** Keep the display on while a workout runs, so the set list is there between sets. */
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    fun setKeepScreenOn(on: Boolean) = putBoolean(KEY_SCREEN_ON, on, _keepScreenOn)

    private val _restVibrate = MutableStateFlow(prefs.getBoolean(KEY_REST_VIBRATE, true))
    val restVibrate: StateFlow<Boolean> = _restVibrate.asStateFlow()

    fun setRestVibrate(on: Boolean) = putBoolean(KEY_REST_VIBRATE, on, _restVibrate)

    private val _restSound = MutableStateFlow(prefs.getBoolean(KEY_REST_SOUND, false))
    val restSound: StateFlow<Boolean> = _restSound.asStateFlow()

    fun setRestSound(on: Boolean) = putBoolean(KEY_REST_SOUND, on, _restSound)

    private val _logRir = MutableStateFlow(prefs.getBoolean(KEY_LOG_RIR, false))
    /** Show a reps-in-reserve chip on every set (for people who train by RIR). */
    val logRir: StateFlow<Boolean> = _logRir.asStateFlow()

    fun setLogRir(on: Boolean) = putBoolean(KEY_LOG_RIR, on, _logRir)

    private val _crashReports = MutableStateFlow(prefs.getBoolean(KEY_CRASH_REPORTS, false))
    /** Off until the person switches it on (DSGVO). */
    val crashReports: StateFlow<Boolean> = _crashReports.asStateFlow()

    fun setCrashReports(on: Boolean) = putBoolean(KEY_CRASH_REPORTS, on, _crashReports)

    /** Whether we already asked for the notification permission (once; after that only from Settings). */
    var notificationsAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ASKED, value) }

    /** A random id for this installation, used as the document id under users/{uid}/devices. */
    val installId: String
        get() = prefs.getString(KEY_INSTALL_ID, null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit { putString(KEY_INSTALL_ID, it) }
        }

    private fun putBoolean(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        prefs.edit { putBoolean(key, value) }
        flow.value = value
    }

    private inline fun <reified E : Enum<E>> enumOr(key: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SCREEN_ON = "keepScreenOn"
        const val KEY_REST_VIBRATE = "restVibrate"
        const val KEY_REST_SOUND = "restSound"
        const val KEY_CRASH_REPORTS = "crashReports"
        const val KEY_INSTALL_ID = "installId"
        const val KEY_LOG_RIR = "logRir"
        const val KEY_NOTIFICATIONS_ASKED = "notificationsAsked"
    }
}
