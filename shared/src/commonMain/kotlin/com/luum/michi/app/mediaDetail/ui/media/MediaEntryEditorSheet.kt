package com.luum.michi.app.mediaDetail.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.label
import com.luum.michi.app.mediaDetail.ui.media.components.EditorCounterField
import com.luum.michi.app.mediaDetail.ui.media.components.StatusChipRail
import com.luum.michi.app.mediaDetail.ui.media.components.EditorToggleRow
import com.luum.michi.app.mediaDetail.ui.media.state.MediaEntryEditorState
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.GlassButton
import com.luum.michi.app.ui.components.SheetTextField
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.PrimaryButton
import com.luum.michi.app.ui.components.SheetActionBar
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Full entry editor as a bottom sheet (not a screen): status, progress,
 * score, notes, repeat, priority, flags, dates, favourite, save and
 * delete. Takes the holder (like SettingsScreen takes SettingsState) —
 * primitives would be a 30-param call. Applies nothing until Save;
 * the lists refresh on [onSaved]/[onDeleted].
 */
@Composable
internal fun MediaEntryEditorSheet(
    editor: MediaEntryEditorState,
    formatScore: (Float) -> String,
    parseScore: (String) -> Float?,
    scoreSuffix: String?,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val busy = editor.isSaving || editor.isDeleting || editor.isLoadingDetail
    ModalSheet(
        // No title: the media title opens the body, actions float right.
        title = "",
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
        fullscreen = true,
        headerActions = listOf(
            ToolbarAction(
                id = ACTION_FAVOURITE,
                icon = if (editor.isFavourite) {
                    AppIcons.FavoriteFilled
                } else {
                    AppIcons.Favorite
                },
                contentDescription = strings.favouriteLabel,
            ),
        ),
        onHeaderAction = { id ->
            if (id == ACTION_FAVOURITE) editor.toggleFavourite()
        },
        footer = {
            if (!editor.isLoadingDetail && editor.detail != null) {
                SheetActionBar {
                    if (editor.isExisting) {
                        Box(modifier = Modifier.weight(1f)) {
                            GlassButton(
                                label = strings.mediaDetailEditorDeleteAction,
                                onClick = { confirmDelete = true },
                                enabled = !busy,
                                contentColor = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryButton(
                            label = strings.saveAction,
                            onClick = { editor.save(onSaved) },
                            enabled = !busy,
                        )
                    }
                }
            }
        },
    ) {
        when {
            editor.isLoadingDetail -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            editor.loadError != null -> {
                MessagePanel(
                    title = strings.mediaDetailErrorLabel,
                    message = editor.loadError?.let { strings.networkErrorMessage(it) },
                    icon = Icons.Filled.Warning,
                    actionLabel = strings.retryAction,
                    onAction = { editor.load() },
                )
            }
            else -> {
                EditorBody(
                    editor = editor,
                    formatScore = formatScore,
                    parseScore = parseScore,
                    scoreSuffix = scoreSuffix,
                    strings = strings,
                )
                if (editor.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = editor.error?.let { strings.networkErrorMessage(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ModalSheet(
            title = strings.deleteEntryConfirmTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { confirmDelete = false },
            footer = {
                SheetActionBar {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassButton(
                            label = strings.confirmDeleteAction,
                            onClick = {
                                editor.delete {
                                    confirmDelete = false
                                    onDeleted()
                                }
                            },
                            enabled = !busy,
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        ) {
            Text(
                text = strings.deleteEntryConfirmMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditorBody(
    editor: MediaEntryEditorState,
    formatScore: (Float) -> String,
    parseScore: (String) -> Float?,
    scoreSuffix: String?,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = editor.detail?.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.statusLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusChipRail(
            options = MediaListStatus.entries,
            labels = MediaListStatus.entries.map { it.label(strings, editor.isManga) },
            selected = editor.status,
            onSelect = { editor.updateStatus(it) },
        )
        Spacer(modifier = Modifier.height(16.dp))
            if (editor.isManga) {
                EditorCounterField(
                    label = strings.chaptersLabel,
                    displayValue = editor.progress.toString(),
                    suffix = editor.maxProgress?.let { "/$it" },
                    onCommit = { raw ->
                        raw.toIntOrNull()?.let { editor.updateProgress(it) }
                    },
                    onDecrement = { editor.updateProgress(editor.progress - 1) },
                    onIncrement = { editor.incrementProgress() },
                    decrementEnabled = editor.progress > 0,
                )
                Spacer(modifier = Modifier.height(16.dp))
                EditorCounterField(
                    label = strings.volumesLabel,
                    displayValue = editor.progressVolumes.toString(),
                    suffix = editor.maxProgressVolumes?.let { "/$it" },
                    onCommit = { raw ->
                        raw.toIntOrNull()?.let { editor.updateProgressVolumes(it) }
                    },
                    onDecrement = { editor.updateProgressVolumes(editor.progressVolumes - 1) },
                    onIncrement = { editor.incrementProgressVolumes() },
                    decrementEnabled = editor.progressVolumes > 0,
                )
            } else {
                EditorCounterField(
                    label = strings.mediaDetailEpisodesLabel,
                    displayValue = editor.progress.toString(),
                    suffix = editor.maxProgress?.let { "/$it" },
                    onCommit = { raw ->
                        raw.toIntOrNull()?.let { editor.updateProgress(it) }
                    },
                    onDecrement = { editor.updateProgress(editor.progress - 1) },
                    onIncrement = { editor.incrementProgress() },
                    decrementEnabled = editor.progress > 0,
                )
            }
        Spacer(modifier = Modifier.height(16.dp))
        EditorCounterField(
            label = strings.scoreLabel,
            displayValue = formatScore(editor.score),
            suffix = scoreSuffix,
            onCommit = { raw ->
                parseScore(raw)?.let { editor.updateScore(it) }
            },
            onDecrement = {
                editor.updateScore((editor.score - SCORE_STEP).coerceAtLeast(0f))
            },
            onIncrement = { editor.updateScore(editor.score + SCORE_STEP) },
            decrementEnabled = editor.score > 0f,
        )
        if (editor.showAdvancedScoring) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.settingsAdvancedScoringTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            editor.advancedScoringNames.forEachIndexed { index, name ->
                Spacer(modifier = Modifier.height(8.dp))
                val value = editor.advancedScoreValues.getOrNull(index) ?: 0f
                EditorCounterField(
                    label = name,
                    displayValue = formatAdvancedScore(value),
                    suffix = ADVANCED_SCORE_SUFFIX,
                    onCommit = { raw ->
                        raw.toFloatOrNull()?.let { editor.updateAdvancedScore(index, it) }
                    },
                    onDecrement = { editor.updateAdvancedScore(index, value - ADVANCED_SCORE_STEP) },
                    onIncrement = { editor.updateAdvancedScore(index, value + ADVANCED_SCORE_STEP) },
                    decrementEnabled = value > 0f,
                    incrementEnabled = value < ADVANCED_SCORE_MAX,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        DatePickerField(
            label = strings.startedLabel,
            millis = editor.startedAtMillis,
            onConfirm = { editor.updateStartedAt(it) },
            onClear = { editor.updateStartedAt(null) },
            strings = strings,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DatePickerField(
            label = strings.completedLabel,
            millis = editor.completedAtMillis,
            onConfirm = { editor.updateCompletedAt(it) },
            onClear = { editor.updateCompletedAt(null) },
            strings = strings,
        )

        Spacer(modifier = Modifier.height(16.dp))
        EditorCounterField(
            label = strings.repeatLabel,
            displayValue = editor.repeat.toString(),
            suffix = null,
            onCommit = { raw ->
                raw.toIntOrNull()?.let { editor.updateRepeat(it) }
            },
            onDecrement = { editor.decrementRepeat() },
            onIncrement = { editor.incrementRepeat() },
            decrementEnabled = editor.repeat > 0,
        )
        Spacer(modifier = Modifier.height(16.dp))
        EditorCounterField(
            label = strings.priorityLabel,
            displayValue = editor.priority.toString(),
            suffix = null,
            onCommit = { raw ->
                raw.toIntOrNull()?.let { editor.updatePriority(it) }
            },
            onDecrement = { editor.decrementPriority() },
            onIncrement = { editor.incrementPriority() },
            decrementEnabled = editor.priority > 0,
            incrementEnabled = editor.priority < 5,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = null) {
            EditorToggleRow(
                title = strings.privateLabel,
                checked = editor.isPrivate,
                onCheckedChange = { editor.updatePrivate(it) },
                divider = false,
            )
            EditorToggleRow(
                title = strings.hiddenFromStatusListsLabel,
                checked = editor.hiddenFromStatusLists,
                onCheckedChange = { editor.updateHiddenFromStatusLists(it) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.notesLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SheetTextField(
            value = editor.notes,
            onValueChange = { editor.updateNotes(it) },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val SCORE_STEP = 0.5f

private const val ADVANCED_SCORE_STEP = 1f
private const val ADVANCED_SCORE_MAX = 100f
private const val ADVANCED_SCORE_SUFFIX = "/100"

/** Trims float noise: 80 (not 80.0), keeps real decimals (87.5). */
private fun formatAdvancedScore(value: Float): String {
    val rounded = value.toLong()
    return if (value == rounded.toFloat()) rounded.toString() else value.toString()
}

private const val ACTION_FAVOURITE = "favourite"

/**
 * Date button opening the system-style M3 calendar dialog (CLEAR /
 * CANCEL / OK, like the reference editor). Single dialog host per field;
 * OK with no selection changes nothing, CLEAR wipes the date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    millis: Long?,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        GhostButton(
            label = millis?.let { formatDateLabel(it) } ?: strings.selectDateAction,
            onClick = { showPicker = true },
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onConfirm)
                        showPicker = false
                    },
                ) {
                    Text(text = strings.datePickerOkAction)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onClear()
                            showPicker = false
                        },
                    ) {
                        Text(text = strings.datePickerClearAction)
                    }
                    TextButton(onClick = { showPicker = false }) {
                        Text(text = strings.cancelAction)
                    }
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private val MONTH_ABBR = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun formatDateLabel(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return "${date.day.toString().padStart(2, '0')}-${MONTH_ABBR[date.month.ordinal]}-${date.year}"
}
