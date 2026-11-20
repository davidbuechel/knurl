package de.knurl.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.core.settings.AppLanguage
import de.knurl.core.settings.AppPreferences
import de.knurl.core.settings.ThemeMode
import de.knurl.data.NotificationSettings
import de.knurl.data.Settings
import de.knurl.data.WeightUnit
import de.knurl.feature.common.Formatting
import de.knurl.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DeviceSettings(
    val theme: ThemeMode,
    val language: AppLanguage,
    val keepScreenOn: Boolean,
    val restVibrate: Boolean,
    val restSound: Boolean,
    val crashReports: Boolean,
)

class SettingsViewModel(
    private val training: TrainingRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    val settings: StateFlow<Settings?> = training.data.filterNotNull().map { it.settings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val language = MutableStateFlow(prefs.language)

    val device: StateFlow<DeviceSettings> = combine(
        prefs.theme, language, prefs.keepScreenOn, combine(prefs.restVibrate, prefs.restSound, ::Pair), prefs.crashReports,
    ) { theme, lang, screenOn, (vibrate, sound), crash ->
        DeviceSettings(theme, lang, screenOn, vibrate, sound, crash)
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        DeviceSettings(prefs.theme.value, prefs.language, prefs.keepScreenOn.value, prefs.restVibrate.value, prefs.restSound.value, prefs.crashReports.value),
    )

    fun update(transform: (Settings) -> Settings) = training.updateSettings(transform)

    fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings) =
        training.updateSettings { it.copy(notifications = transform(it.notifications)) }

    /** Switching units also switches a standard bar: 20 kg ↔ 45 lb, so the plate math stays round. */
    fun setUnit(unit: WeightUnit) = training.updateSettings { s ->
        val bar = when {
            unit == WeightUnit.LB && s.barKg == STANDARD_BAR_KG -> Formatting.fromUnit(STANDARD_BAR_LB, WeightUnit.LB)
            unit == WeightUnit.KG && Math.round(Formatting.toUnit(s.barKg, WeightUnit.LB)) == STANDARD_BAR_LB.toLong() -> STANDARD_BAR_KG
            else -> s.barKg
        }
        s.copy(unit = unit, barKg = bar)
    }

    fun setTheme(mode: ThemeMode) = prefs.setTheme(mode)

    fun setLanguage(value: AppLanguage) {
        language.value = value
        prefs.setLanguage(value) // recreates the activity in the new language
    }

    fun setKeepScreenOn(on: Boolean) = prefs.setKeepScreenOn(on)
    fun setRestVibrate(on: Boolean) = prefs.setRestVibrate(on)
    fun setRestSound(on: Boolean) = prefs.setRestSound(on)
    fun setCrashReports(on: Boolean) = prefs.setCrashReports(on)

    private companion object {
        const val STANDARD_BAR_KG = 20.0
        const val STANDARD_BAR_LB = 45.0
    }
}
