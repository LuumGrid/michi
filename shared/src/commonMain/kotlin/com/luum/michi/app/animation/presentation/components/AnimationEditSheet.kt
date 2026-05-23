package com.luum.michi.app.animation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.animation.presentation.model.isCompleted
import com.luum.michi.app.animation.presentation.model.modalLabel
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformBooleanRow
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformDateField
import com.luum.michi.app.core.platform.components.PlatformModalSheet
import com.luum.michi.app.core.platform.components.PlatformScoreField
import com.luum.michi.app.core.platform.components.PlatformStepperField

private val AnimationEditStatusOptions: List<AnimationListSection> = listOf(
    AnimationListSection.WATCHING,
    AnimationListSection.PLANNING,
    AnimationListSection.COMPLETED_TV,
    AnimationListSection.DROPPED,
    AnimationListSection.PAUSED,
    AnimationListSection.REWATCHING,
)

@Composable
internal fun AnimationEditSheet(
    entry: AnimationListEntry,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var selectedStatus by remember(entry.id) { mutableStateOf(entry.status) }
    var progress by remember(entry.id) { mutableStateOf(entry.progress) }
    var score by remember(entry.id) { mutableStateOf(entry.score.toFloatOrNull() ?: 0f) }
    var notes by remember(entry.id) { mutableStateOf("") }
    var repeat by remember(entry.id) { mutableStateOf(0) }
    var isPrivate by remember(entry.id) { mutableStateOf(false) }
    var hiddenFromStatusLists by remember(entry.id) { mutableStateOf(false) }

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.82f,
    ) { sheetModifier ->
        Box(modifier = sheetModifier.imePadding()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Text(
                        text = strings.statusLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    PlatformChips(
                        items = AnimationEditStatusOptions,
                        selectedItem = selectedStatus,
                        onSelect = { selectedStatus = it },
                        label = { it.modalLabel(strings) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    PlatformStepperField(
                        label = strings.progressLabel,
                        value = progress.toString(),
                        onMinus = { if (progress > 0) progress-- },
                        onPlus = {
                            val max = entry.totalEpisodes
                            progress = if (max == null) progress + 1 else minOf(progress + 1, max)
                        },
                    )
                }
                item {
                    PlatformScoreField(score = score, onScoreChange = { score = it })
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notesLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        singleLine = false,
                    )
                }
                item { PlatformDateField(label = strings.startedLabel, value = "2 Jul 2024") }
                item {
                    PlatformDateField(
                        label = strings.completedLabel,
                        value = if (selectedStatus.isCompleted) strings.todayLabel else "",
                    )
                }
                item {
                    PlatformStepperField(
                        label = strings.repeatLabel,
                        value = repeat.toString(),
                        onMinus = { if (repeat > 0) repeat-- },
                        onPlus = { repeat++ },
                    )
                }
                item {
                    PlatformBooleanRow(
                        label = strings.privateLabel,
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                    )
                }
                item {
                    PlatformBooleanRow(
                        label = strings.hiddenFromStatusListsLabel,
                        checked = hiddenFromStatusLists,
                        onCheckedChange = { hiddenFromStatusLists = it },
                    )
                }
            }

            AnimationEditActionBar(
                onRemove = onDismiss,
                onSave = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnimationEditActionBar(
    onRemove: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRemove) {
                Icon(
                    painter = PlatformIcons.Close,
                    contentDescription = null,
                    tint = Color(0xFFFFB4AB),
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.removeAction,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = onSave) {
                Icon(
                    painter = PlatformIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.saveAction,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
