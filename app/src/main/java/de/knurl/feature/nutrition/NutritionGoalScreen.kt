package de.knurl.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.data.Sex
import de.knurl.domain.model.NutritionGoal
import de.knurl.domain.model.WeightGoal
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.NumberInputDialog
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.SegmentedRow
import de.knurl.feature.common.ValueRow
import de.knurl.feature.common.appContainer
import de.knurl.feature.common.weightText
import de.knurl.feature.common.LocalWeightUnit
import androidx.compose.runtime.LaunchedEffect
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import java.time.Year

private enum class Editing { WEIGHT, HEIGHT, YEAR, KCAL, PROTEIN }

/**
 * The daily targets: goal, body weight, and optionally height, age and sex for a better estimate.
 * The result is shown at once; calories and protein can also be set by hand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionGoalScreen(go: Navigator) {
    val training = appContainer().training
    val data by training.data.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Editing?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_goal_title)) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        val d = data ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        val settings = d.settings
        val unit = LocalWeightUnit.current
        // Opening this page sets nutrition up (with "keep weight" until changed).
        LaunchedEffect(settings.nutrition == null) { if (settings.nutrition == null) training.updateSettings { it.copy(nutrition = NutritionGoal()) } }
        val goal = settings.nutrition ?: NutritionGoal()
        fun setGoal(transform: (NutritionGoal) -> NutritionGoal) = training.updateSettings { it.copy(nutrition = transform(it.nutrition ?: NutritionGoal())) }
        val targets = targetsFor(d.copy(settings = settings.copy(nutrition = goal)))

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ListGroup {
                    SegmentedRow(
                        stringResource(R.string.nutrition_goal_question), WeightGoal.entries, goal.goal,
                        { stringResource(when (it) { WeightGoal.LOSE -> R.string.goal_lose; WeightGoal.MAINTAIN -> R.string.goal_keep; WeightGoal.GAIN -> R.string.goal_gain }) },
                        { g -> setGoal { it.copy(goal = g) } }, divider = false,
                    )
                }
            }

            // The result, before the details: what the day should look like.
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.nutrition_your_day).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (targets == null) {
                        Text(stringResource(R.string.nutrition_need_weight), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text(Formatting.kg(targets.kcal.toDouble()), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(stringResource(R.string.nutrition_kcal_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column {
                                Text("${targets.proteinG} g", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(stringResource(R.string.nutrition_protein), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Text(
                            stringResource(R.string.nutrition_estimate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.nutrition_about_you)) }
            item {
                ListGroup {
                    ValueRow(stringResource(R.string.body_weight), d.bodyWeights.lastOrNull()?.let { weightText(it.weightKg) } ?: "–", onClick = { editing = Editing.WEIGHT })
                    ValueRow(stringResource(R.string.nutrition_height), goal.heightCm?.let { "$it cm" } ?: "–", onClick = { editing = Editing.HEIGHT })
                    ValueRow(stringResource(R.string.profile_birth_year), settings.birthYear?.toString() ?: "–", onClick = { editing = Editing.YEAR })
                    SegmentedRow(stringResource(R.string.profile_sex), listOf(null, Sex.FEMALE, Sex.MALE), settings.sex, {
                        stringResource(when (it) { null -> R.string.profile_sex_none; Sex.FEMALE -> R.string.profile_sex_female; Sex.MALE -> R.string.profile_sex_male })
                    }, { sex -> training.updateSettings { it.copy(sex = sex) } }, divider = false)
                }
            }
            item {
                Text(
                    pluralStringResource(R.plurals.nutrition_activity_hint, settings.weeklyGoal, settings.weeklyGoal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item { SectionHeader(stringResource(R.string.nutrition_by_hand)) }
            item {
                ListGroup {
                    ValueRow(
                        stringResource(R.string.nutrition_kcal_label),
                        goal.kcalOverride?.let { Formatting.kg(it.toDouble()) } ?: stringResource(R.string.nutrition_auto),
                        onClick = { editing = Editing.KCAL },
                    )
                    ValueRow(
                        stringResource(R.string.nutrition_protein),
                        goal.proteinOverride?.let { "$it g" } ?: stringResource(R.string.nutrition_auto),
                        onClick = { editing = Editing.PROTEIN },
                        divider = false,
                    )
                }
            }
            if (goal.kcalOverride != null || goal.proteinOverride != null) {
                item {
                    TextButton(onClick = { setGoal { it.copy(kcalOverride = null, proteinOverride = null) } }) {
                        Text(stringResource(R.string.nutrition_back_to_auto))
                    }
                }
            }
        }

        val thisYear = Year.now().value
        when (editing) {
            Editing.WEIGHT -> NumberInputDialog(
                stringResource(R.string.body_weight_in, Formatting.symbol(unit)),
                d.bodyWeights.lastOrNull()?.let { Formatting.weightNumber(it.weightKg, unit) } ?: "", decimal = true,
                range = Math.round(Formatting.toUnit(20.0, unit)).toDouble()..Math.round(Formatting.toUnit(400.0, unit)).toDouble(),
                onDismiss = { editing = null }, onConfirm = { training.addBodyWeight(Formatting.fromUnit(it, unit)); editing = null },
            )
            Editing.HEIGHT -> NumberInputDialog(
                stringResource(R.string.nutrition_height_cm), goal.heightCm?.toString() ?: "", decimal = false, range = 120.0..230.0,
                onDismiss = { editing = null }, onConfirm = { v -> setGoal { it.copy(heightCm = v.toInt()) }; editing = null },
            )
            Editing.YEAR -> NumberInputDialog(
                stringResource(R.string.profile_birth_year), settings.birthYear?.toString() ?: "", decimal = false,
                range = (thisYear - 100).toDouble()..(thisYear - 13).toDouble(),
                onDismiss = { editing = null }, onConfirm = { v -> training.updateSettings { it.copy(birthYear = v.toInt()) }; editing = null },
            )
            Editing.KCAL -> NumberInputDialog(
                stringResource(R.string.nutrition_kcal_label), (goal.kcalOverride ?: targets?.kcal)?.toString() ?: "", decimal = false, range = 1000.0..6000.0,
                onDismiss = { editing = null }, onConfirm = { v -> setGoal { it.copy(kcalOverride = v.toInt()) }; editing = null },
            )
            Editing.PROTEIN -> NumberInputDialog(
                stringResource(R.string.protein_grams), (goal.proteinOverride ?: targets?.proteinG)?.toString() ?: "", decimal = false, range = 30.0..400.0,
                onDismiss = { editing = null }, onConfirm = { v -> setGoal { it.copy(proteinOverride = v.toInt()) }; editing = null },
            )
            null -> Unit
        }
    }
}
