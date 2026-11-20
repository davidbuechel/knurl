package de.knurl.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import de.knurl.domain.model.BodyWeight
import de.knurl.feature.common.ConfirmDialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.ui.navigation.RecapRoute
import de.knurl.feature.common.Formatting
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.weightText
import de.knurl.ui.brand.BrandLoadingBox
import de.knurl.ui.navigation.Navigator

/** A pushed page with a back arrow and a list; shared by the progress sub pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressPage(go: Navigator, title: String, content: LazyListScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun BodyWeightScreen(go: Navigator) {
    val vm = knurlViewModel { ProgressViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<BodyWeight?>(null) }
    ProgressPage(go, stringResource(R.string.body_weight)) {
        val s = state ?: return@ProgressPage item { BrandLoadingBox() }
        item { BodyWeightCard(s.bodyWeights, onAdd = vm::addBodyWeight) }
        if (s.bodyWeights.isNotEmpty()) item {
            ListGroup {
                val entries = s.bodyWeights.reversed()
                entries.forEachIndexed { i, entry ->
                    ListRow(
                        title = weightText(entry.weightKg),
                        subtitle = Formatting.date(entry.atMs),
                        divider = i < entries.lastIndex,
                        trailing = {
                            IconButton(onClick = { deleting = entry }) {
                                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
            }
        }
    }
    deleting?.let { entry ->
        ConfirmDialog(
            title = stringResource(R.string.body_weight_delete_title),
            body = stringResource(R.string.body_weight_delete_body, weightText(entry.weightKg), Formatting.date(entry.atMs)),
            confirm = stringResource(R.string.action_delete),
            onDismiss = { deleting = null },
            onConfirm = { vm.deleteBodyWeight(entry.id); deleting = null },
        )
    }
}

@Composable
fun CalendarScreen(go: Navigator) {
    val vm = knurlViewModel { ProgressViewModel(it.training) }
    val state by vm.state.collectAsStateWithLifecycle()
    ProgressPage(go, stringResource(R.string.calendar)) {
        val s = state ?: return@ProgressPage item { BrandLoadingBox() }
        item { TrainingCalendarCard(s.trainingDays, onRecap = { go.to(RecapRoute(it.year, it.monthValue)) }) }
    }
}
