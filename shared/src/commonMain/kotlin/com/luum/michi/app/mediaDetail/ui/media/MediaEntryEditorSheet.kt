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
import com.luum.michi.app.ui.components.DatePickerField
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
    scoreStep: Float,
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
                    scoreStep = scoreStep,
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
    scoreStep: Float,
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
                editor.updateScore((editor.score - scoreStep).coerceAtLeast(0f))
            },
            onIncrement = { editor.updateScore(editor.score + scoreStep) },
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
                    displayValue = formatScore(value),
                    suffix = scoreSuffix,
                    onCommit = { raw ->
                        parseScore(raw)?.let { editor.updateAdvancedScore(index, it) }
                    },
                    onDecrement = { editor.updateAdvancedScore(index, value - ADVANCED_SCORE_STEP) },
                    onIncrement = { editor.updateAdvancedScore(index, value + ADVANCED_SCORE_STEP) },
                    decrementEnabled = value > 0f,
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

private const val ACTION_FAVOURITE = "favourite"

// Advanced rows always draft in 0–10 (server scale is fixed 0–100),
// so their step is fixed too — unlike the main row, which steps in
// the user's score format via [scoreStep].
private const val ADVANCED_SCORE_STEP = 0.5f
