package de.knurl.feature.recap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import de.knurl.R
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.achievements.MonthRecap
import de.knurl.domain.achievements.MonthRecaps
import de.knurl.domain.model.Exercise
import de.knurl.feature.achievements.achievements
import de.knurl.feature.achievements.title
import de.knurl.feature.common.Kpi
import de.knurl.feature.common.KpiRow
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.estimateText
import de.knurl.feature.common.isGerman
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.tonnageText
import de.knurl.feature.exercises.ExerciseThumbnail
import de.knurl.ui.brand.BodyMap
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.brand.KnurlMark
import de.knurl.ui.brand.Medal
import de.knurl.ui.navigation.Navigator
import de.knurl.ui.theme.Knurl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import java.io.File
import java.time.format.TextStyle
import kotlin.math.roundToInt

class MonthRecapViewModel(private val training: TrainingRepository, private val year: Int, private val month: Month) : ViewModel() {
    val state: StateFlow<MonthRecap?> = training.data.filterNotNull().map { data ->
        MonthRecaps.build(year, month, data.workouts, data.achievements(), TimeZone.currentSystemDefault(), training::exercise)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exercise(id: String): Exercise? = training.exercise(id)
}

/**
 * The month as a story, made to be shared: the card at the top is drawn once more into a picture
 * (the same composable, no second layout) and handed to the share sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthRecapScreen(go: Navigator, year: Int, monthNumber: Int) {
    val month = Month.entries[monthNumber - 1]
    val vm = knurlViewModel(key = "recap-$year-$monthNumber") { MonthRecapViewModel(it.training, year, month) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val german = isGerman()
    val locale = LocalConfiguration.current.locales[0]
    val monthName = java.time.Month.of(monthNumber).getDisplayName(TextStyle.FULL_STANDALONE, locale)
    val shareTitle = stringResource(R.string.recap_share_title, monthName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recap_title)) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = layer.toImageBitmap().asAndroidBitmap()
                        withContext(Dispatchers.IO) { share(context, bitmap, shareTitle) }
                    }
                },
                enabled = state != null,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(R.string.recap_share), modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) { padding ->
        val r = state ?: return@Scaffold BrandLoadingBox(Modifier.padding(padding))
        // The picture that gets shared: an invisible full-height copy of the card, recorded but never
        // drawn, so scrolling or the bottom bar can't cut it off. Margin and background included.
        Box(Modifier.fillMaxWidth().height(0.dp)) {
            Column(
                Modifier.fillMaxWidth().wrapContentHeight(unbounded = true, align = Alignment.Top)
                    .drawWithContent { layer.record { this@drawWithContent.drawContent() } }
                    .background(MaterialTheme.colorScheme.background).padding(16.dp),
            ) { RecapCard(r, monthName, year, vm::exercise, german) }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { RecapCard(r, monthName, year, vm::exercise, german) }
            if (r.unlocked.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.recap_unlocked)) }
                item {
                    ListGroup {
                        r.unlocked.forEachIndexed { i, status ->
                            ListRow(
                                status.achievement.title(),
                                leading = { Medal(status.achievement.kind, status.achievement.tier, true, 1f, size = 44.dp) },
                                divider = i < r.unlocked.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapCard(r: MonthRecap, monthName: String, year: Int, exercise: (String) -> Exercise?, german: Boolean) {
    val brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brush).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recap_eyebrow, year).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                KnurlMark(Modifier.size(26.dp), color = MaterialTheme.colorScheme.onPrimary)
            }
            Text(stringResource(R.string.recap_headline, monthName), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
            Text(
                pluralStringResource(R.plurals.recap_summary, r.stats.workouts, r.stats.workouts, r.stats.trainingDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
        }
        KpiRow(
            listOf(
                Kpi(tonnageText(r.stats.volumeKg), stringResource(R.string.stat_volume)),
                Kpi("${(r.stats.minutes / 60.0).roundToInt()} h", stringResource(R.string.stat_time)),
                Kpi("${r.stats.records}", stringResource(R.string.stat_records)),
            ),
        )
        ListGroup {
            val favorite = r.favoriteExerciseId?.let(exercise)
            if (favorite != null) {
                ListRow(
                    favorite.name(german),
                    subtitle = pluralStringResource(R.plurals.recap_favorite, r.favoriteSets, r.favoriteSets),
                    leading = { ExerciseThumbnail(favorite, Modifier.size(44.dp)) },
                    divider = r.topGain != null,
                )
            }
            r.topGain?.let { gain ->
                val ex = exercise(gain.exerciseId)
                ListRow(
                    ex?.name(german) ?: gain.exerciseId,
                    subtitle = stringResource(R.string.recap_gain, estimateText(gain.fromKg), estimateText(gain.toKg)),
                    leading = { ex?.let { ExerciseThumbnail(it, Modifier.size(44.dp)) } },
                    trailing = { Text("+${gain.percent.roundToInt()} %", style = MaterialTheme.typography.titleSmall, color = Knurl.colors.success) },
                    divider = false,
                )
            }
        }
        Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp)) {
            Text(stringResource(R.string.progress_muscles), style = MaterialTheme.typography.titleSmall)
            BodyMap(r.muscleLevels, Modifier.padding(top = 8.dp))
        }
    }
}

/** Writes the picture to the shared cache folder and opens the share sheet. */
private fun share(context: Context, bitmap: Bitmap, title: String) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "knurl-recap.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
