package de.knurl.feature.achievements

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import de.knurl.R
import de.knurl.domain.achievements.Achievement
import de.knurl.domain.achievements.AchievementKind
import de.knurl.domain.achievements.Achievements
import de.knurl.domain.achievements.Tier
import de.knurl.domain.catalog.ExerciseCatalog
import de.knurl.data.WeightUnit
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.LocalWeightUnit
import de.knurl.feature.common.estimateText
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.tonnageText

/** "100 workouts", "Bench Press 100 kg", "Early bird". */
@Composable
fun Achievement.title(): String {
    val n = target.toInt()
    return when (kind) {
        AchievementKind.WORKOUTS -> if (n == 1) stringResource(R.string.ach_first_workout) else pluralStringResource(R.plurals.ach_workouts_title, n, n)
        AchievementKind.STREAK -> pluralStringResource(R.plurals.ach_streak_title, n, n)
        AchievementKind.SESSION_VOLUME -> stringResource(R.string.ach_session_volume_title, goalTonnes(target))
        AchievementKind.TOTAL_VOLUME -> stringResource(R.string.ach_total_volume_title, goalTonnes(target))
        AchievementKind.BENCH, AchievementKind.SQUAT, AchievementKind.DEADLIFT -> stringResource(R.string.ach_lift_title, liftName(), estimateText(target))
        AchievementKind.RECORDS -> pluralStringResource(R.plurals.ach_records_title, n, n)
        AchievementKind.SUPERSET -> stringResource(R.string.ach_superset_title)
        AchievementKind.EARLY_BIRD -> stringResource(R.string.ach_early_title)
        AchievementKind.HYROX -> stringResource(R.string.ach_hyrox_title)
        AchievementKind.WEIGH_INS -> pluralStringResource(R.plurals.ach_weighins_title, n, n)
    }
}

@Composable
fun Achievement.description(): String {
    val n = target.toInt()
    return when (kind) {
        AchievementKind.WORKOUTS -> pluralStringResource(R.plurals.ach_workouts_desc, n, n)
        AchievementKind.STREAK -> pluralStringResource(R.plurals.ach_streak_desc, n, n)
        AchievementKind.SESSION_VOLUME -> stringResource(R.string.ach_session_volume_desc, goalTonnes(target))
        AchievementKind.TOTAL_VOLUME -> stringResource(R.string.ach_total_volume_desc, goalTonnes(target))
        AchievementKind.BENCH, AchievementKind.SQUAT, AchievementKind.DEADLIFT -> stringResource(R.string.ach_lift_desc, estimateText(target))
        AchievementKind.RECORDS -> pluralStringResource(R.plurals.ach_records_desc, n, n)
        AchievementKind.SUPERSET -> stringResource(R.string.ach_superset_desc)
        AchievementKind.EARLY_BIRD -> stringResource(R.string.ach_early_desc)
        AchievementKind.HYROX -> stringResource(R.string.ach_hyrox_desc)
        AchievementKind.WEIGH_INS -> pluralStringResource(R.plurals.ach_weighins_desc, n, n)
    }
}

/** Progress in the achievement's own unit: "7 of 10", "6.2 t of 10 t", "95 kg of 100 kg". */
@Composable
fun Achievement.progressText(progress: Double): String = when (kind) {
    AchievementKind.SESSION_VOLUME, AchievementKind.TOTAL_VOLUME -> stringResource(R.string.achievement_progress, tonnageText(progress), goalTonnes(target))
    AchievementKind.BENCH, AchievementKind.SQUAT, AchievementKind.DEADLIFT -> stringResource(R.string.achievement_progress, estimateText(progress), estimateText(target))
    else -> stringResource(R.string.achievement_progress, progress.toInt().toString(), target.toInt().toString())
}

@Composable
fun Tier.label(): String = stringResource(
    when (this) {
        Tier.BRONZE -> R.string.tier_bronze
        Tier.SILVER -> R.string.tier_silver
        Tier.GOLD -> R.string.tier_gold
    },
)

@Composable
private fun Achievement.liftName(): String {
    val id = when (kind) {
        AchievementKind.BENCH -> Achievements.BENCH_ID
        AchievementKind.SQUAT -> Achievements.SQUAT_ID
        else -> Achievements.DEADLIFT_ID
    }
    return ExerciseCatalog.find(id)?.name(isGerman()) ?: id
}

/** A round volume goal without false decimals: "5 t", "1.000 t" (pounds as the usual tonnage). */
@Composable
private fun goalTonnes(kg: Double): String =
    if (LocalWeightUnit.current == WeightUnit.KG) "${Formatting.kg(kg / 1000)}${Formatting.NBSP}t" else tonnageText(kg)
