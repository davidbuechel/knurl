package de.knurl.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import androidx.core.net.toUri
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.SegmentedRow
import de.knurl.feature.common.SwitchRow
import de.knurl.feature.common.ValueRow
import de.knurl.ui.theme.Knurl
import de.knurl.BuildConfig
import de.knurl.core.debug.DemoData
import de.knurl.feature.common.appContainer
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
    val logRir by vm.logRir.collectAsStateWithLifecycle()
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
        val s = settings ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionHeader(stringResource(R.string.settings_section_display)) }
            item {
                ListGroup {
                    SegmentedRow(stringResource(R.string.settings_theme), ThemeMode.entries, device.theme, { it.label() }, vm::setTheme)
                    SegmentedRow(stringResource(R.string.settings_language), AppLanguage.entries, device.language, { it.label() }, vm::setLanguage, divider = false)
                }
            }

            item { SectionHeader(stringResource(R.string.settings_training)) }
            item {
                ListGroup {
                    SegmentedRow(stringResource(R.string.settings_unit), WeightUnit.entries, s.unit, { Formatting.symbol(it) }, vm::setUnit)
                    ValueRow(stringResource(R.string.settings_bar_weight), weightText(s.barKg), onClick = { editor = Editor.BAR })
                    // In pounds the plate calculator uses the usual pound plates; the kilogram set only matters in kg.
                    if (s.unit == WeightUnit.KG) {
                        ValueRow(
                            stringResource(R.string.settings_plates),
                            pluralStringResource(R.plurals.settings_plates_value, s.platesKg.size, s.platesKg.size),
                            subtitle = stringResource(R.string.settings_plates_hint),
                            onClick = { editor = Editor.PLATES },
                        )
                    }
                    ValueRow(stringResource(R.string.settings_default_rest), Formatting.duration(s.defaultRestSec.toLong()), onClick = { editor = Editor.REST })
                    ValueRow(stringResource(R.string.settings_weekly_goal), "${s.weeklyGoal}", onClick = { editor = Editor.GOAL }, divider = false)
                }
            }
            item {
                ListGroup {
                    SwitchRow(stringResource(R.string.settings_keep_screen_on), device.keepScreenOn, vm::setKeepScreenOn, stringResource(R.string.settings_keep_screen_on_hint))
                    SwitchRow(stringResource(R.string.settings_log_rir), logRir, vm::setLogRir, stringResource(R.string.settings_log_rir_hint))
                    SwitchRow(stringResource(R.string.settings_rest_vibrate), device.restVibrate, vm::setRestVibrate)
                    SwitchRow(stringResource(R.string.settings_rest_sound), device.restSound, vm::setRestSound, divider = false)
                }
            }

            item { SectionHeader(stringResource(R.string.settings_section_notifications)) }
            item { NotificationGroups(s, vm, onDays = { editor = Editor.DAYS }, onTime = { editor = Editor.TIME }) }

            item { SectionHeader(stringResource(R.string.settings_section_privacy)) }
            item {
                ListGroup {
                    SwitchRow(stringResource(R.string.settings_crash_reports), device.crashReports, vm::setCrashReports, stringResource(R.string.settings_crash_reports_hint), divider = false)
                }
            }

            item { SectionHeader(stringResource(R.string.settings_section_legal)) }
            item {
                ListGroup {
                    ListRow(stringResource(R.string.legal_imprint), leading = { IconTile(Icons.Outlined.Gavel, MaterialTheme.colorScheme.outline) }) { go.to(LegalRoute(LegalPage.IMPRINT)) }
                    ListRow(stringResource(R.string.legal_privacy), leading = { IconTile(Icons.Outlined.Policy, MaterialTheme.colorScheme.outline) }) { go.to(LegalRoute(LegalPage.PRIVACY)) }
                    ListRow(stringResource(R.string.legal_licenses), leading = { IconTile(Icons.AutoMirrored.Outlined.Article, MaterialTheme.colorScheme.outline) }, divider = false) { go.to(LegalRoute(LegalPage.LICENSES)) }
                }
            }

            // Debug builds only: test data for statistics and achievements.
            if (BuildConfig.DEBUG) {
                item { SectionHeader(stringResource(R.string.debug_section)) }
                item {
                    val container = appContainer()
                    ListGroup {
                        ListRow(stringResource(R.string.debug_demo_data), subtitle = stringResource(R.string.debug_demo_data_hint), divider = false) {
                            DemoData.fill(container.training)
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        SettingsEditors(editor, s, vm, close = { editor = null })
    }
}

@Composable
private fun NotificationGroups(s: Settings, vm: SettingsViewModel, onDays: () -> Unit, onTime: () -> Unit) {
    val n = s.notifications
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Notifications.permissionGranted(context)) }
    val alarms = context.getSystemService(AlarmManager::class.java)
    var exactAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) }
    // Coming back from the system settings: both permissions may have changed there.
    LifecycleResumeEffect(Unit) {
        granted = Notifications.permissionGranted(context)
        exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        onPauseOrDispose {}
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    // Switching a notification on asks for the permission first, if Android needs it.
    fun toggle(on: Boolean, change: (NotificationSettings) -> NotificationSettings) {
        vm.updateNotifications(change)
        if (on && !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val needsExactAlarms = n.restTimer && !exactAllowed

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!granted || needsExactAlarms) {
            ListGroup {
                if (!granted) {
                    ListRow(
                        stringResource(R.string.notify_blocked), subtitle = stringResource(R.string.notify_blocked_hint),
                        leading = { IconTile(Icons.Outlined.NotificationsOff, MaterialTheme.colorScheme.error) }, divider = needsExactAlarms,
                    ) {
                        context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                }
                if (needsExactAlarms) {
                    ListRow(
                        stringResource(R.string.notify_exact), subtitle = stringResource(R.string.notify_exact_hint),
                        leading = { IconTile(Icons.Outlined.Alarm, Knurl.colors.warning) }, divider = false,
                    ) {
                        context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, ("package:" + context.packageName).toUri()))
                    }
                }
            }
        }
        ListGroup {
            SwitchRow(stringResource(R.string.notify_rest), n.restTimer, { on -> toggle(on) { it.copy(restTimer = on) } }, stringResource(R.string.notify_rest_hint))
            SwitchRow(stringResource(R.string.notify_running), n.workoutRunning, { on -> toggle(on) { it.copy(workoutRunning = on) } }, stringResource(R.string.notify_running_hint), divider = false)
        }
        ListGroup {
            SwitchRow(stringResource(R.string.notify_reminders), n.reminders, { on -> toggle(on) { it.copy(reminders = on) } }, stringResource(R.string.notify_reminders_hint), divider = n.reminders)
            if (n.reminders) {
                ValueRow(stringResource(R.string.notify_reminder_days), n.reminderDays.sorted().joinToString(", ") { dayName(it, context) }, onClick = onDays)
                ValueRow(stringResource(R.string.notify_reminder_time), Formatting.timeOfDay(n.reminderMinutes), onClick = onTime, divider = false)
            }
        }
        ListGroup {
            SwitchRow(stringResource(R.string.notify_streak), n.streak, { on -> toggle(on) { it.copy(streak = on) } }, stringResource(R.string.notify_streak_hint))
            SwitchRow(stringResource(R.string.notify_weekly), n.weeklyReview, { on -> toggle(on) { it.copy(weeklyReview = on) } }, stringResource(R.string.notify_weekly_hint))
            SwitchRow(stringResource(R.string.notify_account), n.account, { on -> toggle(on) { it.copy(account = on) } }, stringResource(R.string.notify_account_hint))
            SwitchRow(stringResource(R.string.notify_news), n.news, { on -> toggle(on) { it.copy(news = on) } }, stringResource(R.string.notify_news_hint), divider = false)
        }
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
                            label = { Text("${Formatting.kg(plate)} ${Formatting.symbol(WeightUnit.KG)}") },
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
