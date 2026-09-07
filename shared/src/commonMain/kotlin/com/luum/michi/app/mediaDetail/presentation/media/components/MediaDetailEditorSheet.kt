package com.luum.michi.app.mediaDetail.presentation.media.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.ui.Icons
import com.luum.michi.app.ui.components.BooleanRow
import com.luum.michi.app.ui.components.Chips
import com.luum.michi.app.ui.components.DatePickerField
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.ScoreField
import com.luum.michi.app.ui.components.SheetActionBar
import com.luum.michi.app.ui.components.StepperField
import com.luum.michi.app.core.anilist.medialist.MediaListStatus
import com.luum.michi.app.core.anilist.medialist.label
import com.luum.michi.app.mediaDetail.presentation.media.state.MediaEntryEditorState

private val EditorStatusOptions: List<MediaListStatus> = listOf(
    MediaListStatus.CURRENT,
    MediaListStatus.PLANNING,
    MediaListStatus.COMPLETED,
    MediaListStatus.PAUSED,
    MediaListStatus.DROPPED,
    MediaListStatus.REPEATING,
)

@Composable
internal fun MediaDetailEditorSheet(
    state: MediaEntryEditorState,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteEntryConfirmTitle) },
            text = {
                Column {
                    Text(strings.deleteEntryConfirmMessage)
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            state.delete(onDeleted)
                        }) {
                            Text(
                                text = strings.confirmDeleteAction,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(strings.mediaDetailEditorCancelAction)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    ModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
    ) { sheetModifier ->
        Column(modifier = sheetModifier.imePadding()) {
            EditorHeader(
                title = strings.mediaDetailEditorTitleEdit,
                isFavourite = state.isFavourite,
                onToggleFavourite = state::toggleFavourite,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoadingDetail -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    state.loadError != null -> Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.loadError?.let { strings.networkErrorMessage(it) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> EditorForm(state = state)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.error != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = strings.mediaDetailEditorSaveErrorLabel + ": " +
                                (state.error?.let { strings.networkErrorMessage(it) } ?: ""),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                SheetActionBar(
                    leadingLabel = if (state.isExisting) strings.mediaDetailEditorDeleteAction else strings.mediaDetailEditorCancelAction,
                    trailingLabel = if (state.isSaving) strings.mediaDetailEditorSavingLabel else strings.saveAction,
                    onLeadingClick = if (state.isExisting) { { showDeleteConfirm = true } } else onDismiss,
                    onTrailingClick = { state.save(onSaved) },
                    leadingDestructive = state.isExisting,
                    leadingEnabled = !state.isSaving && !state.isDeleting,
                    trailingEnabled = !state.isLoadingDetail && state.loadError == null && !state.isSaving && !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        androidx.compose.material3.IconButton(
            onClick = onToggleFavourite,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                painter = if (isFavourite) Icons.LikeFilled else Icons.Like,
                contentDescription = null,
                tint = if (isFavourite) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditorForm(state: MediaEntryEditorState) {
    val strings = LanguageProvider.strings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = strings.statusLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        item {
            Chips(
                items = EditorStatusOptions,
                selectedItem = state.status,
                onSelect = state::updateStatus,
                label = { it.label(strings, state.isManga) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val progressLabel = if (state.isManga) strings.chaptersLabel else strings.progressLabel
            val suffix = state.maxProgress?.let { " / $it" } ?: ""
            StepperField(
                label = progressLabel + suffix,
                value = state.progress.toString(),
                onMinus = state::decrementProgress,
                onPlus = state::incrementProgress,
            )
        }
        if (state.isManga) {
            item {
                val suffix = state.maxProgressVolumes?.let { " / $it" } ?: ""
                StepperField(
                    label = strings.volumesLabel + suffix,
                    value = state.progressVolumes.toString(),
                    onMinus = state::decrementProgressVolumes,
                    onPlus = state::incrementProgressVolumes,
                )
            }
        }
        item {
            ScoreField(
                score = state.score,
                onScoreChange = state::updateScore,
            )
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = state::updateNotes,
                label = { Text(strings.notesLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                singleLine = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        item {
            StepperField(
                label = if (state.isManga) strings.totalRereadsLabel else strings.totalRewatchesLabel,
                value = state.repeat.toString(),
                onMinus = state::decrementRepeat,
                onPlus = state::incrementRepeat,
            )
        }
        item {
            StepperField(
                label = strings.priorityLabel + " (0–5)",
                value = state.priority.toString(),
                onMinus = state::decrementPriority,
                onPlus = state::incrementPriority,
            )
        }
        item {
            DatePickerField(
                label = strings.startedLabel,
                valueMillis = state.startedAtMillis,
                onValueChange = state::updateStartedAt,
            )
        }
        item {
            DatePickerField(
                label = strings.completedLabel,
                valueMillis = state.completedAtMillis,
                onValueChange = state::updateCompletedAt,
            )
        }
        item {
            BooleanRow(
                label = strings.privateLabel,
                checked = state.isPrivate,
                onCheckedChange = state::updatePrivate,
            )
        }
        item {
            BooleanRow(
                label = strings.hiddenFromStatusListsLabel,
                checked = state.hiddenFromStatusLists,
                onCheckedChange = state::updateHiddenFromStatusLists,
            )
        }
    }
}

