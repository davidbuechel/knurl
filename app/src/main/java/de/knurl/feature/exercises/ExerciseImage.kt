package de.knurl.feature.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.core.image.ExercisePhotos
import de.knurl.feature.common.appContainer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.knurl.BuildConfig
import de.knurl.R
import de.knurl.domain.model.Exercise
import de.knurl.ui.brand.KnurlMark
import kotlinx.coroutines.delay

fun Exercise.imageUrl(index: Int): String? =
    if (index < images) "${BuildConfig.EXERCISE_IMAGES_URL}/$id/$index.webp" else null

/** The photo of an own exercise, decoded once; null without one. Follows changes from other devices. */
@Composable
fun rememberOwnPhoto(exercise: Exercise): ImageBitmap? {
    if (!exercise.isCustom) return null
    val training = appContainer().training
    val base64 by remember(exercise.id) {
        training.data.map { it?.exercisePhotos?.get(exercise.id) }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(training.photo(exercise.id))
    return remember(base64) { base64?.let(ExercisePhotos::decode) }
}

/**
 * Small square photo for lists. Own exercises show their photo or their initial on a cobalt tile;
 * catalog exercises without a photo show the mark.
 */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    val own = rememberOwnPhoto(exercise)
    Box(
        modifier.size(56.dp).clip(MaterialTheme.shapes.medium)
            .background(if (exercise.isCustom && own == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val url = exercise.imageUrl(0)
        when {
            own != null -> Image(own, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            exercise.isCustom -> Text(
                exercise.nameEn.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            url != null -> AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> KnurlMark(Modifier.fillMaxSize(0.5f), color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * The movement as a small gallery: swipe between the positions, dots show where you are. Playing
 * through them only happens when asked (play/pause), never on its own.
 */
@Composable
fun ExerciseGallery(exercise: Exercise, modifier: Modifier = Modifier) {
    val own = rememberOwnPhoto(exercise)
    if (own != null) {
        Image(
            own, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)),
        )
        return
    }
    if (exercise.images == 0) return
    val pager = rememberPagerState { exercise.images }
    var playing by rememberSaveable(exercise.id) { mutableStateOf(false) }
    LaunchedEffect(playing, exercise.id) {
        while (playing && exercise.images > 1) {
            delay(1_200)
            pager.animateScrollToPage((pager.currentPage + 1) % exercise.images)
        }
    }
    Box(modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            AsyncImage(exercise.imageUrl(page), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (exercise.images > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(exercise.images) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.height(6.dp).width(if (on) 18.dp else 6.dp).clip(CircleShape).background(Color.White.copy(alpha = if (on) 1f else 0.55f)))
                }
            }
            FilledIconButton(
                onClick = { playing = !playing },
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.55f), contentColor = Color.White),
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.gallery_pause else R.string.gallery_play),
                )
            }
        }
    }
}
