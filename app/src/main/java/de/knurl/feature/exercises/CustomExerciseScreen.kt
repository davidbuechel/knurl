package de.knurl.feature.exercises

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.knurl.R
import de.knurl.core.image.ExercisePhotos
import de.knurl.data.repository.TrainingRepository
import de.knurl.domain.catalog.CustomExercises
import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Muscle
import de.knurl.feature.common.ConfirmDialog
import de.knurl.feature.common.IconTile
import de.knurl.feature.common.ListGroup
import de.knurl.feature.common.ListRow
import de.knurl.feature.common.SectionHeader
import de.knurl.feature.common.knurlViewModel
import de.knurl.feature.common.label
import de.knurl.ui.brand.BrandBusyOverlay
import de.knurl.ui.navigation.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CustomExerciseForm(
    val name: String = "",
    val primary: Muscle = Muscle.CHEST,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Equipment = Equipment.MACHINE,
    val note: String = "",
    /** A newly chosen photo (base64 WebP), not saved yet. */
    val newPhoto: String? = null,
    val removePhoto: Boolean = false,
)

class CustomExerciseViewModel(private val training: TrainingRepository, private val exerciseId: String?, initialName: String) : ViewModel() {

    private val existing = exerciseId?.let(training::exercise)?.takeIf { it.isCustom }
    private val original = existing?.let {
        CustomExerciseForm(it.nameEn, it.primary, it.secondary, it.equipment, it.note)
    } ?: CustomExerciseForm(name = initialName.trim().take(CustomExercises.NAME_MAX))

    private val _form = MutableStateFlow(original)
    val form: StateFlow<CustomExerciseForm> = _form.asStateFlow()

    val editing: Boolean get() = existing != null
    val savedPhoto: String? get() = existing?.let { training.photo(it.id) }

    /** Used anywhere: deleting it only archives it. */
    val used: Boolean
        get() = existing != null && training.data.value?.let { CustomExercises.isUsed(existing.id, it.workouts, it.routines, it.active) } == true

    fun update(change: (CustomExerciseForm) -> CustomExerciseForm) = _form.update(change)

    fun toggleSecondary(muscle: Muscle) = _form.update { f ->
        val next = if (muscle in f.secondary) f.secondary - muscle else f.secondary + muscle
        f.copy(secondary = next.filter { it != f.primary }.take(CustomExercises.SECONDARY_MAX))
    }

    fun hasChanges(): Boolean = _form.value != original

    /** Saves and returns the exercise id. */
    fun save(): String {
        val f = _form.value
        val id = existing?.id ?: training.newCustomExerciseId()
        training.saveCustomExercise(
            CustomExercises.build(id, f.name, f.primary, f.secondary, f.equipment, f.note),
            photo = f.newPhoto,
            removePhoto = f.removePhoto,
        )
        return id
    }

    fun delete() {
        existing?.let { training.removeCustomExercise(it.id) }
    }
}

/**
 * Creating or editing an own exercise. With [pickForWorkout] (opened from the picker), saving hands
 * the new exercise straight back to the workout or routine that asked for one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseScreen(go: Navigator, exerciseId: String?, initialName: String, pickForWorkout: Boolean) {
    val vm = knurlViewModel(key = "custom-$exerciseId") { CustomExerciseViewModel(it.training, exerciseId, initialName) }
    val form by vm.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.custom_photo_failed)
    var busy by remember { mutableStateOf(false) }
    var photoSheet by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Text fields own their text (a flow-bound field can lose letters when typing fast).
    var name by rememberSaveable { mutableStateOf(form.name) }
    var note by rememberSaveable { mutableStateOf(form.note) }

    fun usePhoto(uri: Uri) = scope.launch {
        busy = true
        val encoded = withContext(Dispatchers.Default) { ExercisePhotos.fromUri(context, uri) }
        busy = false
        if (encoded != null) vm.update { it.copy(newPhoto = encoded, removePhoto = false) } else snackbar.showSnackbar(failedText)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::usePhoto) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(::usePhoto) }
    fun takePhoto() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(dir, "exercise.jpg"))
        cameraUri = uri
        camera.launch(uri)
    }

    val leave = { if (vm.hasChanges()) confirmDiscard = true else go.back() }
    // Only with unsaved changes: otherwise the back gesture plays its normal animation.
    BackHandler(enabled = vm.hasChanges()) { confirmDiscard = true }
    val canSave = name.isNotBlank()
    fun save() {
        val id = vm.save()
        if (pickForWorkout) go.backWithResult(Navigator.RESULT_EXERCISE, id) else go.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.editing) R.string.custom_edit else R.string.custom_new)) },
                navigationIcon = {
                    IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    Button(onClick = ::save, enabled = canSave, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val shown = when {
                        form.newPhoto != null -> form.newPhoto
                        form.removePhoto -> null
                        else -> vm.savedPhoto
                    }
                    PhotoTile(shown, onClick = { photoSheet = true })
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { v -> name = v.take(CustomExercises.NAME_MAX); vm.update { it.copy(name = name) } },
                        label = { Text(stringResource(R.string.custom_name)) },
                        placeholder = { Text(stringResource(R.string.custom_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                }

                item { SectionHeader(stringResource(R.string.custom_primary)) }
                item {
                    ChipGroup {
                        Muscle.entries.forEach { muscle ->
                            FilterChip(
                                selected = form.primary == muscle,
                                onClick = { vm.update { it.copy(primary = muscle, secondary = it.secondary - muscle) } },
                                label = { Text(stringResource(muscle.label())) },
                            )
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.custom_secondary)) }
                item {
                    ChipGroup(hint = stringResource(R.string.custom_secondary_hint)) {
                        Muscle.entries.filter { it != form.primary }.forEach { muscle ->
                            val on = muscle in form.secondary
                            FilterChip(
                                selected = on,
                                enabled = on || form.secondary.size < CustomExercises.SECONDARY_MAX,
                                onClick = { vm.toggleSecondary(muscle) },
                                label = { Text(stringResource(muscle.label())) },
                            )
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.custom_equipment)) }
                item {
                    ChipGroup {
                        Equipment.entries.forEach { equipment ->
                            FilterChip(
                                selected = form.equipment == equipment,
                                onClick = { vm.update { it.copy(equipment = equipment) } },
                                label = { Text(stringResource(equipment.label())) },
                            )
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.custom_note)) }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { v -> note = v.take(CustomExercises.NOTE_MAX); vm.update { it.copy(note = note) } },
                        placeholder = { Text(stringResource(R.string.custom_note_hint)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }

                if (vm.editing) {
                    item {
                        ListGroup(Modifier.padding(top = 10.dp)) {
                            ListRow(
                                stringResource(R.string.custom_delete),
                                leading = { IconTile(Icons.Outlined.Delete, MaterialTheme.colorScheme.error) },
                                divider = false,
                            ) { confirmDelete = true }
                        }
                    }
                }
            }
            BrandBusyOverlay(busy)
        }
    }

    if (photoSheet) {
        ModalBottomSheet(onDismissRequest = { photoSheet = false }) {
            ListGroup(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                ListRow(stringResource(R.string.custom_photo_gallery), leading = { IconTile(Icons.Outlined.PhotoLibrary) }) {
                    photoSheet = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                val hasPhoto = form.newPhoto != null || (!form.removePhoto && vm.savedPhoto != null)
                ListRow(stringResource(R.string.custom_photo_camera), leading = { IconTile(Icons.Outlined.PhotoCamera) }, divider = hasPhoto) {
                    photoSheet = false
                    takePhoto()
                }
                if (hasPhoto) {
                    ListRow(stringResource(R.string.custom_photo_remove), leading = { IconTile(Icons.Outlined.Delete, MaterialTheme.colorScheme.error) }, divider = false) {
                        photoSheet = false
                        vm.update { it.copy(newPhoto = null, removePhoto = true) }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.custom_delete),
            body = stringResource(if (vm.used) R.string.custom_delete_used else R.string.custom_delete_unused),
            confirm = stringResource(R.string.action_delete),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.delete()
                // The detail page of a deleted exercise has nothing left to show.
                go.back(times = if (pickForWorkout) 1 else 2)
            },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.routine_discard_title),
            body = stringResource(R.string.custom_discard_body),
            confirm = stringResource(R.string.action_discard_changes),
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; go.back() },
        )
    }
}

/** The photo, or an invitation to add one. */
@Composable
private fun PhotoTile(base64: String?, onClick: () -> Unit) {
    val bitmap = remember(base64) { base64?.let(ExercisePhotos::decode) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(148.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Text(
            stringResource(if (bitmap != null) R.string.custom_photo_change else R.string.custom_photo_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onClick).padding(top = 8.dp, bottom = 2.dp),
        )
        Text(stringResource(R.string.custom_photo_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Chips on the group surface, with an optional hint above them. */
@Composable
private fun ChipGroup(hint: String? = null, content: @Composable () -> Unit) {
    ListGroup {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}
