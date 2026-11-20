package de.knurl.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.BuildConfig
import de.knurl.R
import de.knurl.core.settings.AppLanguage
import de.knurl.core.settings.ThemeMode
import de.knurl.core.notifications.Notifications
import de.knurl.data.NotificationSettings
import de.knurl.data.Settings
import de.knurl.data.WeightUnit
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.weightNumber
import de.knurl.feature.common.weightText
import de.knurl.feature.legal.LegalPage
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.LegalRoute
import de.knurl.ui.navigation.Navigator
import java.time.DayOfWeek
import java.time.format.TextStyle

private enum class Editor { BAR, REST, GOAL, PLATES, DAYS, TIME }

/** Plates a gym may have; the person ticks the ones theirs has. */
private val PlateOptionsKg = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 2.0, 1.25, 1.0, 0.5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(go: Navigator) {
    val vm = knurlViewModel { SettingsViewModel(it.training, it.preferences) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = go::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val s = settings ?: return@Scaffold BrandLoadingBox()
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsSection(stringResource(R.string.settings_section_display)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
                    Segmented(ThemeMode.entries, device.theme, { it.label() }, vm::setTheme)
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
                    Segmented(AppLanguage.entries, device.language, { it.label() }, vm::setLanguage)
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_training)) {
                    Text(stringResource(R.string.settings_unit), style = MaterialTheme.typography.titleSmall)
                    Segmented(WeightUnit.entries, s.unit, { Formatting.symbol(it) }, vm::setUnit)
                    ValueRow(stringResource(R.string.settings_bar_weight), weightText(s.barKg)) { editor = Editor.BAR }
                    RowDivider()
                    ValueRow(
                        stringResource(R.string.settings_plates),
                        stringResource(R.string.settings_plates_value, s.platesKg.size),
                        stringResource(R.string.settings_plates_hint),
                    ) { editor = Editor.PLATES }
                    RowDivider()
                    ValueRow(stringResource(R.string.settings_default_rest), Formatting.duration(s.defaultRestSec.toLong())) { editor = Editor.REST }
                    RowDivider()
                    ValueRow(stringResource(R.string.settings_weekly_goal), "${s.weeklyGoal}") { editor = Editor.GOAL }
                    RowDivider()
                    ToggleRow(stringResource(R.string.settings_keep_screen_on), stringResource(R.string.settings_keep_screen_on_hint), device.keepScreenOn, onChange = vm::setKeepScreenOn)
                    val logRir by vm.logRir.collectAsStateWithLifecycle()
                    ToggleRow(stringResource(R.string.settings_log_rir), stringResource(R.string.settings_log_rir_hint), logRir, onChange = vm::setLogRir)
                    ToggleRow(stringResource(R.string.settings_rest_vibrate), null, device.restVibrate, onChange = vm::setRestVibrate)
                    ToggleRow(stringResource(R.string.settings_rest_sound), null, device.restSound, onChange = vm::setRestSound)
                }
            }

            item { NotificationSection(s, vm, onDays = { editor = Editor.DAYS }, onTime = { editor = Editor.TIME }) }

            item {
                SettingsSection(stringResource(R.string.settings_section_privacy)) {
                    ToggleRow(stringResource(R.string.settings_crash_reports), stringResource(R.string.settings_crash_reports_hint), device.crashReports, onChange = vm::setCrashReports)
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_section_legal)) {
                    LinkRow(Icons.Outlined.Gavel, stringResource(R.string.legal_imprint)) { go.to(LegalRoute(LegalPage.IMPRINT)) }
                    LinkRow(Icons.Outlined.Policy, stringResource(R.string.legal_privacy)) { go.to(LegalRoute(LegalPage.PRIVACY)) }
                    LinkRow(Icons.AutoMirrored.Outlined.Article, stringResource(R.string.legal_licenses)) { go.to(LegalRoute(LegalPage.LICENSES)) }
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }

        SettingsEditors(editor, s, vm, close = { editor = null })
    }
}

@Composable
private fun NotificationSection(s: Settings, vm: SettingsViewModel, onDays: () -> Unit, onTime: () -> Unit) {
    val n = s.notifications
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Notifications.permissionGranted(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    // Switching a notification on asks for the permission first, if Android needs it.
    fun enable(change: (NotificationSettings) -> NotificationSettings) {
        vm.updateNotifications(change)
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun toggle(on: Boolean, change: (NotificationSettings) -> NotificationSettings) = if (on) enable(change) else vm.updateNotifications(change)
    val alarms = context.getSystemService(AlarmManager::class.java)
    SettingsSection(stringResource(R.string.settings_section_notifications)) {
        if (!granted) {
            LinkRow(Icons.Outlined.NotificationsOff, stringResource(R.string.notify_blocked), stringResource(R.string.notify_blocked_hint), tint = MaterialTheme.colorScheme.error) {
                context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }
        ToggleRow(stringResource(R.string.notify_rest), stringResource(R.string.notify_rest_hint), n.restTimer) { on -> toggle(on) { it.copy(restTimer = on) } }
        if (n.restTimer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            LinkRow(Icons.Outlined.Alarm, stringResource(R.string.notify_exact), stringResource(R.string.notify_exact_hint)) {
                context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)))
            }
        }
        ToggleRow(stringResource(R.string.notify_running), stringResource(R.string.notify_running_hint), n.workoutRunning) { on -> toggle(on) { it.copy(workoutRunning = on) } }
        RowDivider()
        ToggleRow(stringResource(R.string.notify_reminders), stringResource(R.string.notify_reminders_hint), n.reminders) { on -> toggle(on) { it.copy(reminders = on) } }
        if (n.reminders) {
            ValueRow(stringResource(R.string.notify_reminder_days), n.reminderDays.sorted().joinToString(", ") { dayName(it, context) }, onClick = onDays)
            ValueRow(stringResource(R.string.notify_reminder_time), Formatting.timeOfDay(n.reminderMinutes), onClick = onTime)
        }
        RowDivider()
        ToggleRow(stringResource(R.string.notify_streak), stringResource(R.string.notify_streak_hint), n.streak) { on -> toggle(on) { it.copy(streak = on) } }
        ToggleRow(stringResource(R.string.notify_weekly), stringResource(R.string.notify_weekly_hint), n.weeklyReview) { on -> toggle(on) { it.copy(weeklyReview = on) } }
        RowDivider()
        ToggleRow(stringResource(R.string.notify_account), stringResource(R.string.notify_account_hint), n.account) { on -> toggle(on) { it.copy(account = on) } }
        ToggleRow(stringResource(R.string.notify_news), stringResource(R.string.notify_news_hint), n.news) { on -> toggle(on) { it.copy(news = on) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsEditors(editor: Editor?, s: Settings, vm: SettingsViewModel, close: () -> Unit) {
    val context = LocalContext.current
    when (editor) {
        Editor.BAR -> NumberInputDialog(
            stringResource(R.string.settings_bar_weight_in, Formatting.symbol(s.unit)), weightNumber(s.barKg), decimal = true,
            onDismiss = close, onConfirm = { v -> vm.update { it.copy(barKg = Formatting.fromUnit(v, s.unit)) }; close() },
        )
        Editor.REST -> NumberInputDialog(
            stringResource(R.string.settings_default_rest_seconds), "${s.defaultRestSec}", decimal = false,
            onDismiss = close, onConfirm = { v -> vm.update { it.copy(defaultRestSec = v.toInt().coerceIn(15, 900)) }; close() },
        )
        Editor.GOAL -> NumberInputDialog(
            stringResource(R.string.settings_weekly_goal), "${s.weeklyGoal}", decimal = false,
            onDismiss = close, onConfirm = { v -> vm.update { it.copy(weeklyGoal = v.toInt().coerceIn(1, 7)) }; close() },
        )
        Editor.PLATES -> AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.settings_plates)) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlateOptionsKg.forEach { plate ->
                        val on = plate in s.platesKg
                        FilterChip(
                            selected = on,
                            onClick = { vm.update { it.copy(platesKg = (if (on) it.platesKg - plate else it.platesKg + plate).sortedDescending()) } },
                            label = { Text("${Formatting.kg(plate)} kg") },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.action_done)) } },
        )
        Editor.DAYS -> AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.notify_reminder_days)) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { day ->
                        val on = day in s.notifications.reminderDays
                        FilterChip(
                            selected = on,
                            onClick = { vm.updateNotifications { it.copy(reminderDays = if (on) it.reminderDays - day else (it.reminderDays + day).sorted()) } },
                            label = { Text(dayName(day, context)) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.action_done)) } },
        )
        Editor.TIME -> {
            val state = rememberTimePickerState(s.notifications.reminderMinutes / 60, s.notifications.reminderMinutes % 60)
            AlertDialog(
                onDismissRequest = close,
                title = { Text(stringResource(R.string.notify_reminder_time)) },
                text = { TimePicker(state) },
                confirmButton = {
                    TextButton(onClick = { vm.updateNotifications { it.copy(reminderMinutes = state.hour * 60 + state.minute) }; close() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
        null -> Unit
    }
}

private fun dayName(isoDay: Int, context: android.content.Context): String =
    DayOfWeek.of(isoDay).getDisplayName(TextStyle.SHORT, context.resources.configuration.locales[0])

@Composable
private fun ThemeMode.label() = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

@Composable
private fun AppLanguage.label() = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> R.string.settings_system
        AppLanguage.GERMAN -> R.string.settings_language_de
        AppLanguage.ENGLISH -> R.string.settings_language_en
    },
)
