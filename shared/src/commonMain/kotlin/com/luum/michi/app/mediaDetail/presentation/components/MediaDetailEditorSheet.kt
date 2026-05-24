package com.luum.michi.app.mediaDetail.presentation.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformBooleanRow
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformModalSheet
import com.luum.michi.app.core.platform.components.PlatformScoreField
import com.luum.michi.app.core.platform.components.PlatformStepperField
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import com.luum.michi.app.mediaDetail.presentation.model.label
import com.luum.michi.app.mediaDetail.presentation.state.MediaEntryEditorState

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
) {
    val strings = LanguageProvider.strings

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
    ) { sheetModifier ->
        Box(modifier = sheetModifier.imePadding()) {
            EditorHeader(title = strings.mediaDetailEditorTitleEdit)

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
                        text = state.loadError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> EditorForm(state = state)
            }

            EditorActionBar(
                isSaving = state.isSaving,
                enabled = !state.isLoadingDetail && state.loadError == null,
                error = state.error,
                onCancel = onDismiss,
                onSave = { state.save(onSaved) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditorHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
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
            top = 24.dp,
            bottom = 120.dp,
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
            PlatformChips(
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
            PlatformStepperField(
                label = progressLabel + suffix,
                value = state.progress.toString(),
                onMinus = state::decrementProgress,
                onPlus = state::incrementProgress,
            )
        }
        if (state.isManga) {
            item {
                val suffix = state.maxProgressVolumes?.let { " / $it" } ?: ""
                PlatformStepperField(
                    label = strings.volumesLabel + suffix,
                    value = state.progressVolumes.toString(),
                    onMinus = state::decrementProgressVolumes,
                    onPlus = state::incrementProgressVolumes,
                )
            }
        }
        item {
            PlatformScoreField(
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
            PlatformStepperField(
                label = strings.repeatLabel,
                value = state.repeat.toString(),
                onMinus = state::decrementRepeat,
                onPlus = state::incrementRepeat,
            )
        }
        item {
            PlatformBooleanRow(
                label = strings.privateLabel,
                checked = state.isPrivate,
                onCheckedChange = state::updatePrivate,
            )
        }
        item {
            PlatformBooleanRow(
                label = strings.hiddenFromStatusListsLabel,
                checked = state.hiddenFromStatusLists,
                onCheckedChange = state::updateHiddenFromStatusLists,
            )
        }
    }
}

@Composable
private fun EditorActionBar(
    isSaving: Boolean,
    enabled: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
                if (error != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = strings.mediaDetailEditorSaveErrorLabel + ": " + error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        text = strings.mediaDetailEditorCancelAction,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = enabled && !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(
                        text = if (isSaving) strings.mediaDetailEditorSavingLabel else strings.saveAction,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

