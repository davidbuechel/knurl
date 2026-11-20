package de.knurl.feature.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import de.knurl.feature.exercises.ExerciseThumbnail
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import de.knurl.domain.calc.Change
import de.knurl.domain.calc.Progress
import de.knurl.domain.calc.SetDelta
import de.knurl.feature.common.HeroCard
import de.knurl.feature.common.Pill
import de.knurl.ui.brand.KnurlProgress
import de.knurl.R
import androidx.compose.material.icons.outlined.MonitorHeart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import de.knurl.data.Sex
import de.knurl.core.health.HeartRate
import de.knurl.core.health.HealthSync
import de.knurl.ui.navigation.BodyWeightRoute
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.Icons
import de.knurl.feature.common.IconTile
import de.knurl.domain.calc.Calories
import de.knurl.ui.navigation.AchievementsRoute
import de.knurl.ui.brand.Medal
import de.knurl.feature.achievements.description
import de.knurl.feature.achievements.title
import de.knurl.feature.achievements.achievements
import de.knurl.domain.achievements.Achievements
import de.knurl.domain.achievements.AchievementStatus
import de.knurl.domain.model.SetEntry
import de.knurl.feature.common.estimateText
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.calc.History
import de.knurl.domain.calc.PersonalRecord
import de.knurl.domain.calc.Volume
import de.knurl.domain.model.Routine
import de.knurl.domain.model.Workout
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.KnurlCard
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.NameDialog
import de.knurl.feature.common.ScreenTitle
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.setText
import de.knurl.feature.common.tonnageText
import de.knurl.feature.common.weightText
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.theme.Knurl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class SummaryState(
    val workout: Workout,
    val durationSec: Long,
    val sets: Int,
    val volumeKg: Double,
    /** Volume of the previous session of the same routine (or name), to compare. */
    val previousVolumeKg: Double?,
    val records: List<PersonalRecord>,
    /** The routine this workout came from, if it still exists. */
    val routine: Routine?,
    val savedAsRoutine: Boolean,
    /** Exercises whose top set beat the previous session: heavier, or more reps at the same weight. */
    val gains: List<Gain>,
    /** Achievements this very workout unlocked. */
    val unlocked: List<AchievementStatus>,
    /** Estimated energy, null without a body weight. */
    val kcal: Int?,
    val heartRate: HeartRate?,
    /** Energy from the heart rate (with age and sex), not from the MET estimate. */
    val kcalFromHeartRate: Int?,
)

/** One exercise that went up: today's top set against last time's. */
data class Gain(val exerciseId: String, val delta: SetDelta, val now: SetEntry, val before: SetEntry)

class SummaryViewModel(private val training: TrainingRepository, private val health: HealthSync, private val workoutId: String) : ViewModel() {

    private val saved = MutableStateFlow(false)

    /** Heart rate from a watch (Health Connect), read once the workout is there. */
    private val heart = MutableStateFlow<HeartRate?>(null)

    init {
        viewModelScope.launch {
            val workout = training.data.filterNotNull().mapNotNull { d -> d.workouts.firstOrNull { it.id == workoutId } }.first()
            heart.value = health.heartRate(workout)
        }
    }

    val state: StateFlow<SummaryState?> = combine(training.data.filterNotNull(), saved, heart) { data, saved, hr ->
        val workout = data.workouts.firstOrNull { it.id == workoutId } ?: return@combine null
        val previous = data.workouts
            .filter { it.id != workout.id && it.startedAtMs < workout.startedAtMs }
            .filter { if (workout.routineId != null) it.routineId == workout.routineId else it.name == workout.name }
            .maxByOrNull { it.startedAtMs }
        SummaryState(
            workout = workout,
            durationSec = ((workout.endedAtMs ?: workout.startedAtMs) - workout.startedAtMs) / 1000,
            sets = workout.exercises.sumOf { it.sets.size },
            volumeKg = Volume.of(workout),
            previousVolumeKg = previous?.let(Volume::of),
            records = History.records(data.workouts).filter { it.workoutId == workout.id },
            routine = workout.routineId?.let { id -> data.routines.firstOrNull { it.id == id } },
            savedAsRoutine = saved,
            unlocked = Achievements.unlockedBy(workout.id, data.achievements()),
            heartRate = hr,
            kcalFromHeartRate = kcalFromHeart(workout, hr, data),
            kcal = kcalFromHeart(workout, hr, data) ?: Calories.estimate(workout, data.bodyWeights),
            gains = workout.exercises.mapNotNull { item ->
                val before = History.sessions(item.exerciseId, data.workouts.filter { it.startedAtMs < workout.startedAtMs }).lastOrNull()?.sets
                    ?: return@mapNotNull null
                val delta = Progress.sinceLast(item.sets, before)?.takeIf { it.improved } ?: return@mapNotNull null
                Gain(item.exerciseId, delta, Progress.topSet(item.sets)!!, Progress.topSet(before)!!)
            }.distinctBy { it.exerciseId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun kcalFromHeart(workout: Workout, hr: HeartRate?, data: de.knurl.data.TrainingData): Int? {
        hr ?: return null
        val weight = Calories.bodyWeightAt(workout.startedAtMs, data.bodyWeights) ?: return null
        val age = data.settings.birthYear?.let { java.time.Year.now().value - it }
        return Calories.fromHeartRate(hr.averageBpm.toDouble(), Calories.activeMinutes(workout), weight, age, data.settings.sex?.let { it == Sex.MALE })
    }

    fun exercise(id: String) = training.exercise(id)

    fun exerciseName(id: String, german: Boolean) = training.exercise(id)?.name(german) ?: id

    fun saveAsRoutine(name: String) {
        val workout = state.value?.workout ?: return
        training.saveAsRoutine(workout, name)
        saved.value = true
    }

    /** The routine takes over what was done today: exercises, order, number of sets. */
    fun updateRoutine() {
        val s = state.value ?: return
        val routine = s.routine ?: return
        training.saveRoutine(de.knurl.domain.RoutineEditing.updateFromWorkout(routine, s.workout))
        saved.value = true
    }
}

/** After a workout: what was done, what improved, and whether to keep it as a routine. */
@Composable
fun SummaryScreen(go: Navigator, workoutId: String) {
    val vm = knurlViewModel(key = "summary-$workoutId") { SummaryViewModel(it.training, it.health, workoutId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var naming by rememberSaveable { mutableStateOf(false) }
    val german = isGerman()

    Scaffold(
        bottomBar = {
            Button(
                onClick = go::back,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.action_done)) }
        },
    ) { padding ->
        val s = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenTitle(stringResource(R.string.summary_title), eyebrow = s.workout.name) }

            item {
                KpiRow(
                    listOf(
                        Kpi(Formatting.length(s.durationSec), stringResource(R.string.summary_duration)),
                        Kpi("${s.sets}", stringResource(R.string.summary_sets)),
                        Kpi(tonnageText(s.volumeKg), stringResource(R.string.stat_volume), s.previousVolumeKg?.let { Change(s.volumeKg, it).percent }),
                    ),
                )
            }
            item {
                ListGroup {
                    s.heartRate?.let { hr ->
                        ListRow(
                            stringResource(R.string.hr_title),
                            subtitle = stringResource(R.string.hr_value, hr.averageBpm.toInt(), hr.maxBpm.toInt()),
                            leading = { IconTile(Icons.Outlined.MonitorHeart, MaterialTheme.colorScheme.tertiary) },
                        )
                    }
                    if (s.kcal != null) {
                        ListRow(
                            stringResource(R.string.kcal_value, Formatting.kg(s.kcal.toDouble())),
                            subtitle = stringResource(if (s.kcalFromHeartRate != null) R.string.kcal_hint_hr else R.string.kcal_hint),
                            leading = { IconTile(Icons.Outlined.Whatshot, Knurl.colors.warning) },
                            divider = false,
                        )
                    } else {
                        ListRow(
                            stringResource(R.string.kcal_title),
                            subtitle = stringResource(R.string.kcal_missing),
                            leading = { IconTile(Icons.Outlined.Whatshot, MaterialTheme.colorScheme.outline) },
                            divider = false,
                            onClick = { go.to(BodyWeightRoute) },
                        )
                    }
                }
            }

            if (s.records.isNotEmpty()) {
                item { RecordMoment(s.records.size) }
            }

            if (s.unlocked.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.summary_unlocked)) }
                item {
                    ListGroup {
                        s.unlocked.forEachIndexed { i, status ->
                            ListRow(
                                status.achievement.title(),
                                subtitle = status.achievement.description(),
                                leading = { Medal(status.achievement.kind, status.achievement.tier, true, 1f, size = 44.dp) },
                                divider = i < s.unlocked.lastIndex,
                                onClick = { go.to(AchievementsRoute) },
                            )
                        }
                    }
                }
            }

            if (s.gains.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.summary_gains)) }
                item {
                    ListGroup {
                        s.gains.forEachIndexed { i, gain ->
                            val (exerciseId, delta) = gain
                            ListRow(
                                vm.exerciseName(exerciseId, german),
                                subtitle = stringResource(R.string.summary_gain_line, setText(gain.now.weightKg, gain.now.reps), setText(gain.before.weightKg, gain.before.reps)),
                                leading = { vm.exercise(exerciseId)?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                                trailing = {
                                    Pill(
                                        if (delta.weightKg > 0) "+" + weightText(delta.weightKg)
                                        else pluralStringResource(R.plurals.reps_gain, delta.reps, delta.reps),
                                        color = Knurl.colors.success,
                                    )
                                },
                                divider = i < s.gains.lastIndex,
                            )
                        }
                    }
                }
            }

            if (s.records.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.summary_records)) }
                item {
                    ListGroup {
                        s.records.forEachIndexed { i, record ->
                            ListRow(
                                vm.exerciseName(record.exerciseId, german),
                                subtitle = setText(record.set.weightKg, record.set.reps),
                                leading = { vm.exercise(record.exerciseId)?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                                trailing = { Text(stringResource(R.string.unit_e1rm, estimateText(record.oneRepMaxKg)), style = MaterialTheme.typography.labelLarge) },
                                divider = i < s.records.lastIndex,
                            )
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.summary_routine)) }
            item {
                KnurlCard {
                    when {
                        s.savedAsRoutine -> Text(stringResource(R.string.summary_routine_saved), color = Knurl.colors.success)
                        s.routine != null -> {
                            Text(stringResource(R.string.summary_update_hint, s.routine.name), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = vm::updateRoutine) { Text(stringResource(R.string.summary_update_routine)) }
                        }
                        else -> {
                            Text(stringResource(R.string.summary_save_hint), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = { naming = true }) { Text(stringResource(R.string.summary_save_routine)) }
                        }
                    }
                }
            }
        }

        if (naming) {
            NameDialog(
                title = stringResource(R.string.summary_save_routine),
                initial = s.workout.name,
                onDismiss = { naming = false },
                onConfirm = { vm.saveAsRoutine(it); naming = false },
            )
        }
    }
}

/**
 * A short moment for new records: the brand's plates pop in one after another, like a bar being
 * loaded, once. Quiet enough for a gym, clear enough to feel.
 */
@Composable
private fun RecordMoment(count: Int) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(56.dp).graphicsLayer { scaleX = 0.4f + 0.6f * pop.value; scaleY = scaleX; rotationZ = (1 - pop.value) * -25f }
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                KnurlProgress(pop.value.coerceIn(0f, 1f), Modifier.size(34.dp), color = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.summary_record_title, count, count), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                Text(stringResource(R.string.summary_record_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            }
        }
    }
}
