package de.knurl.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.knurl.R

/** What the pad edits and how. */
data class PadTarget(
    val title: String,
    /** The value as shown, e.g. "62,5 kg" or "8". */
    val display: String,
    val decimal: Boolean,
    /** Labels of the quick-step keys, e.g. "−2,5" / "+2,5" or "−1" / "+1". */
    val minusLabel: String,
    val plusLabel: String,
    val nextLabel: String,
)

/**
 * Knurl's own number pad: no system keyboard jumping in, keys big enough for gloves, quick ±
 * steps for the equipment, and "Next" that walks weight → reps → next set.
 */
@Composable
fun NumberPad(
    target: PadTarget,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    fun tap(action: () -> Unit): () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(target.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(target.display, style = MaterialTheme.typography.displaySmall)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.pad_close)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Key("1", tap { onDigit('1') }); Key("2", tap { onDigit('2') }); Key("3", tap { onDigit('3') })
                Key(target.minusLabel, tap(onMinus), accent = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Key("4", tap { onDigit('4') }); Key("5", tap { onDigit('5') }); Key("6", tap { onDigit('6') })
                Key(target.plusLabel, tap(onPlus), accent = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Key("7", tap { onDigit('7') }); Key("8", tap { onDigit('8') }); Key("9", tap { onDigit('9') })
                Key(target.nextLabel, tap(onNext), primary = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (target.decimal) Key(",", tap { onDigit('.') }) else Box(Modifier.weight(1f))
                Key("0", tap { onDigit('0') })
                KeyIcon(tap(onBackspace))
                Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.Key(label: String, onClick: () -> Unit, accent: Boolean = false, primary: Boolean = false) {
    val bg = when {
        primary -> MaterialTheme.colorScheme.primary
        accent -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        primary -> MaterialTheme.colorScheme.onPrimary
        accent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium, color = fg, maxLines = 1) }
}

@Composable
private fun RowScope.KeyIcon(onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(R.string.pad_delete)) }
}
