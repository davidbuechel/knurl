package de.knurl.feature.exercises

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.knurl.BuildConfig
import de.knurl.domain.model.Exercise
import de.knurl.ui.brand.KnurlMark
import kotlinx.coroutines.delay

fun Exercise.imageUrl(index: Int): String? =
    if (index < images) "${BuildConfig.EXERCISE_IMAGES_URL}/$id/$index.webp" else null

/** Small square photo for lists; the mark stands in when there is no photo. */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    Box(
        modifier.size(56.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        val url = exercise.imageUrl(0)
        if (url != null) AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else KnurlMark(Modifier.size(24.dp), color = MaterialTheme.colorScheme.outline)
    }
}

/** Start and end position, switching back and forth, so the movement reads without a video. */
@Composable
fun ExerciseMotion(exercise: Exercise, modifier: Modifier = Modifier) {
    if (exercise.images == 0) return
    var frame by remember(exercise.id) { mutableIntStateOf(0) }
    LaunchedEffect(exercise.id) {
        while (exercise.images > 1) {
            delay(1_400)
            frame = 1 - frame
        }
    }
    Crossfade(frame, animationSpec = tween(350), label = "motion", modifier = modifier.fillMaxWidth().aspectRatio(1.5f).clip(MaterialTheme.shapes.large)) { index ->
        AsyncImage(exercise.imageUrl(index), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
