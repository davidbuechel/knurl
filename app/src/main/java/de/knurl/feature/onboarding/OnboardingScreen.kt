package de.knurl.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LooksOne
import androidx.compose.material.icons.rounded.LooksTwo
import androidx.compose.material.icons.rounded.Looks3
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.knurl.R
import de.knurl.data.WeightUnit
import de.knurl.domain.catalog.Experience
import de.knurl.domain.catalog.Goal
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.SegmentedTabs
import de.knurl.feature.common.appContainer
import de.knurl.ui.brand.KnurlProgress
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * First start: three questions, then the app sets up routines, weekly goal, unit and reminders.
 * The plates at the top load up with every step.
 */
@Composable
fun OnboardingScreen() {
    val training = appContainer().training
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf<Goal?>(null) }
    var experience by rememberSaveable { mutableStateOf<Experience?>(null) }
    var days by rememberSaveable { mutableStateOf(listOf(1, 3, 5)) }
    var unit by rememberSaveable { mutableStateOf(WeightUnit.KG) }
    var reminders by rememberSaveable { mutableStateOf(true) }
    val locale = LocalConfiguration.current.locales[0]

    fun finish() = training.completeOnboarding(goal ?: Goal.MUSCLE, experience ?: Experience.BEGINNER, days.sorted(), unit, reminders)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                KnurlProgress((step + 1) / 3f, Modifier.size(36.dp))
                Text(stringResource(R.string.onboarding_step, step + 1, 3), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp).weight(1f))
                TextButton(onClick = ::finish) { Text(stringResource(R.string.onboarding_skip)) }
            }

            AnimatedContent(step, transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "step", modifier = Modifier.weight(1f)) { current ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (current) {
                        0 -> {
                            Title(R.string.onboarding_goal_title, R.string.onboarding_goal_body)
                            Choice(Icons.Rounded.FitnessCenter, R.string.goal_muscle, R.string.goal_muscle_hint, goal == Goal.MUSCLE) { goal = Goal.MUSCLE }
                            Choice(Icons.Rounded.Bolt, R.string.goal_strength, R.string.goal_strength_hint, goal == Goal.STRENGTH) { goal = Goal.STRENGTH }
                            Choice(Icons.Rounded.Favorite, R.string.goal_fitness, R.string.goal_fitness_hint, goal == Goal.FITNESS) { goal = Goal.FITNESS }
                        }
                        1 -> {
                            Title(R.string.onboarding_experience_title, R.string.onboarding_experience_body)
                            Choice(Icons.Rounded.LooksOne, R.string.level_beginner, R.string.experience_beginner_hint, experience == Experience.BEGINNER) { experience = Experience.BEGINNER }
                            Choice(Icons.Rounded.LooksTwo, R.string.level_intermediate, R.string.experience_intermediate_hint, experience == Experience.INTERMEDIATE) { experience = Experience.INTERMEDIATE }
                            Choice(Icons.Rounded.Looks3, R.string.level_expert, R.string.experience_advanced_hint, experience == Experience.ADVANCED) { experience = Experience.ADVANCED }
                        }
                        else -> {
                            Title(R.string.onboarding_days_title, R.string.onboarding_days_body)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..7).forEach { day ->
                                    val on = day in days
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape)
                                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { days = if (on) days - day else days + day },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale).take(2),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                            Text(
                                stringResource(R.string.onboarding_plan_preview, planName(days.size)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(stringResource(R.string.settings_unit), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                            SegmentedTabs(WeightUnit.entries, unit, { Formatting.symbol(it) }, { unit = it })
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { reminders = !reminders }, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.notify_reminders), style = MaterialTheme.typography.titleSmall)
                                    Text(stringResource(R.string.onboarding_reminders_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(reminders, onCheckedChange = { reminders = it })
                            }
                        }
                    }
                }
            }

            val canContinue = when (step) {
                0 -> goal != null
                1 -> experience != null
                else -> days.isNotEmpty()
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) TextButton(onClick = { step-- }, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.action_back)) }
                Button(onClick = { if (step < 2) step++ else finish() }, enabled = canContinue, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text(stringResource(if (step < 2) R.string.pad_next else R.string.onboarding_start), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun planName(days: Int): String = stringResource(
    when {
        days <= 3 -> R.string.plan_full_body
        days == 4 -> R.string.plan_upper_lower
        else -> R.string.plan_ppl
    },
)

@Composable
private fun Title(@StringRes title: Int, @StringRes body: Int) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Choice(icon: ImageVector, @StringRes title: Int, @StringRes hint: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.large)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 1f else 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        }
    }
}
