package com.luum.michi.app.reading.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.*

internal enum class ReadingListSection {
    ALL,
    READING,
    COMPLETED,
    PAUSED,
    DROPPED,
    PLANNING,
    REREADING,
}

private val StatusSections = listOf(
    ReadingListSection.READING,
    ReadingListSection.COMPLETED,
    ReadingListSection.PAUSED,
    ReadingListSection.DROPPED,
    ReadingListSection.PLANNING,
    ReadingListSection.REREADING,
)

private data class ReadingListEntry(
    val id: Int,
    val title: String,
    val format: String,
    val status: ReadingListSection,
    val chaptersProgress: Int,
    val totalChapters: Int?,
    val volumesProgress: Int,
    val totalVolumes: Int?,
    val score: String,
    val nextChapter: String?,
    val palette: List<Color>,
)

private val InitialEntries = listOf(
    ReadingListEntry(
        id = 1,
        title = "Berserk",
        format = "Manga | Dark Fantasy",
        status = ReadingListSection.READING,
        chaptersProgress = 375,
        totalChapters = null,
        volumesProgress = 42,
        totalVolumes = null,
        score = "9.5",
        nextChapter = "New chapter in 2w",
        palette = listOf(Color(0xFF1E293B), Color(0xFF64748B)),
    ),
    ReadingListEntry(
        id = 2,
        title = "Monster",
        format = "Manga | Thriller",
        status = ReadingListSection.COMPLETED,
        chaptersProgress = 162,
        totalChapters = 162,
        volumesProgress = 18,
        totalVolumes = 18,
        score = "9.2",
        nextChapter = null,
        palette = listOf(Color(0xFF450A0A), Color(0xFF991B1B)),
    ),
    ReadingListEntry(
        id = 3,
        title = "Vagabond",
        format = "Manga | Historical",
        status = ReadingListSection.PAUSED,
        chaptersProgress = 327,
        totalChapters = 327,
        volumesProgress = 37,
        totalVolumes = 37,
        score = "9.3",
        nextChapter = "On hiatus",
        palette = listOf(Color(0xFF064E3B), Color(0xFF059669)),
    ),
    ReadingListEntry(
        id = 4,
        title = "Chainsaw Man",
        format = "Manga | Action",
        status = ReadingListSection.READING,
        chaptersProgress = 165,
        totalChapters = null,
        volumesProgress = 17,
        totalVolumes = null,
        score = "8.7",
        nextChapter = "New chapter tomorrow",
        palette = listOf(Color(0xFF7C2D12), Color(0xFFEA580C)),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingScreen(
    selectedSection: ReadingListSection = ReadingListSection.ALL,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
) {
    val strings = LanguageProvider.strings
    val entries = remember { mutableStateListOf<ReadingListEntry>().also { it.addAll(InitialEntries) } }
    var editingEntry by remember { mutableStateOf<ReadingListEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectedSection == ReadingListSection.ALL) {
                StatusSections.forEach { section ->
                    val sectionEntries = entries.filter { it.status == section }
                    if (sectionEntries.isNotEmpty()) {
                        item {
                            PlatformSectionHeader(
                                title = section.label(strings),
                                count = sectionEntries.size,
                            )
                        }
                        items(
                            items = sectionEntries,
                            key = ReadingListEntry::id,
                        ) { entry ->
                            ReadingListCard(
                                entry = entry,
                                onOpen = {},
                                onEdit = { editingEntry = entry },
                                onIncrementChapters = {
                                    val index = entries.indexOf(entry)
                                    if (index != -1) {
                                        entries[index] = entry.incrementedChapters()
                                    }
                                },
                                onIncrementVolumes = {
                                    val index = entries.indexOf(entry)
                                    if (index != -1) {
                                        entries[index] = entry.incrementedVolumes()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                val sectionEntries = entries.filter { it.status == selectedSection }
                items(
                    items = sectionEntries,
                    key = ReadingListEntry::id,
                ) { entry ->
                    ReadingListCard(
                        entry = entry,
                        onOpen = {},
                        onEdit = { editingEntry = entry },
                        onIncrementChapters = {
                            val index = entries.indexOf(entry)
                            if (index != -1) {
                                entries[index] = entry.incrementedChapters()
                            }
                        },
                        onIncrementVolumes = {
                            val index = entries.indexOf(entry)
                            if (index != -1) {
                                entries[index] = entry.incrementedVolumes()
                            }
                        }
                    )
                }
            }
        }

        editingEntry?.let { entry ->
            ReadingEditSheet(
                entry = entry,
                onDismiss = { editingEntry = null },
            )
        }
    }
}

@Composable
private fun ReadingEditSheet(
    entry: ReadingListEntry,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var selectedStatus by remember(entry.id) { mutableStateOf(entry.status) }
    var chaptersProgress by remember(entry.id) { mutableStateOf(entry.chaptersProgress) }
    var volumesProgress by remember(entry.id) { mutableStateOf(entry.volumesProgress) }
    var score by remember(entry.id) { mutableStateOf(entry.score.toFloatOrNull() ?: 0f) }
    val statusOptions = remember {
        listOf(
            ReadingListSection.READING,
            ReadingListSection.PLANNING,
            ReadingListSection.COMPLETED,
            ReadingListSection.DROPPED,
            ReadingListSection.PAUSED,
            ReadingListSection.REREADING,
        )
    }

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
                        items = statusOptions,
                        selectedItem = selectedStatus,
                        onSelect = { selectedStatus = it },
                        label = { it.label(strings) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    PlatformStepperField(
                        label = strings.chaptersLabel,
                        value = chaptersProgress.toString(),
                        onMinus = { if (chaptersProgress > 0) chaptersProgress-- },
                        onPlus = {
                            val max = entry.totalChapters
                            chaptersProgress = if (max == null) chaptersProgress + 1 else minOf(chaptersProgress + 1, max)
                        },
                    )
                }

                item {
                    PlatformStepperField(
                        label = strings.volumesLabel,
                        value = volumesProgress.toString(),
                        onMinus = { if (volumesProgress > 0) volumesProgress-- },
                        onPlus = {
                            val max = entry.totalVolumes
                            volumesProgress = if (max == null) volumesProgress + 1 else minOf(volumesProgress + 1, max)
                        },
                    )
                }

                item {
                    PlatformScoreField(
                        score = score,
                        onScoreChange = { score = it },
                    )
                }

                item {
                    PlatformDateField(label = strings.startedLabel, value = "15 Jan 2024")
                }

                item {
                    PlatformDateField(label = strings.completedLabel, value = if (selectedStatus == ReadingListSection.COMPLETED) strings.todayLabel else "")
                }

                item {
                    PlatformBooleanRow(
                        label = strings.privateLabel,
                        checked = false,
                        onCheckedChange = { },
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
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
                    TextButton(onClick = onDismiss) {
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

                    TextButton(onClick = onDismiss) {
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
    }
}

@Composable
private fun ReadingListCard(
    entry: ReadingListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementChapters: () -> Unit,
    onIncrementVolumes: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformMediaListCard(
        title = entry.title,
        subtitle = entry.format,
        score = entry.score,
        primaryProgressLabel = entry.chaptersProgressLabel(),
        primaryProgressRatio = entry.chaptersProgressRatio(),
        primaryIncrementLabel = "+1 CH",
        secondaryProgressLabel = entry.volumesProgressLabel(),
        secondaryProgressRatio = entry.volumesProgressRatio(),
        secondaryIncrementLabel = "+1 VO",
        palette = entry.palette,
        icon = PlatformIcons.Reading,
        isComplete = entry.isComplete(),
        statusLabel = entry.nextChapter ?: entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementChapters,
        onIncrementSecondary = onIncrementVolumes,
    )
}

@Composable
internal fun ReadingSectionPlatformChips(
    selected: ReadingListSection,
    onSelect: (ReadingListSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    val sections = remember { ReadingListSection.entries }

    PlatformChips(
        items = sections,
        selectedItem = selected,
        onSelect = onSelect,
        label = { section ->
            val count = if (section == ReadingListSection.ALL) {
                InitialEntries.size
            } else {
                InitialEntries.count { it.status == section }
            }
            "${section.label(strings)} $count"
        },
        modifier = modifier.fillMaxWidth(),
    )
}

private fun ReadingListSection.label(strings: LanguageStrings): String = when (this) {
    ReadingListSection.ALL -> strings.sectionAll
    ReadingListSection.READING -> strings.sectionReading
    ReadingListSection.COMPLETED -> strings.sectionCompleted
    ReadingListSection.PAUSED -> strings.sectionPaused
    ReadingListSection.DROPPED -> strings.sectionDropped
    ReadingListSection.PLANNING -> strings.sectionPlanning
    ReadingListSection.REREADING -> strings.sectionRereading
}

private fun ReadingListEntry.chaptersProgressLabel(): String {
    val total = totalChapters?.toString() ?: "?"
    return "$chaptersProgress / $total chs"
}

private fun ReadingListEntry.volumesProgressLabel(): String {
    val total = totalVolumes?.toString() ?: "?"
    return "$volumesProgress / $total vol"
}

private fun ReadingListEntry.chaptersProgressRatio(): Float {
    val total = totalChapters ?: return 0f
    return if (total == 0) 0f else (chaptersProgress.toFloat() / total).coerceIn(0f, 1f)
}

private fun ReadingListEntry.volumesProgressRatio(): Float {
    val total = totalVolumes ?: return 0f
    return if (total == 0) 0f else (volumesProgress.toFloat() / total).coerceIn(0f, 1f)
}

private fun ReadingListEntry.isComplete(): Boolean {
    val chaptersDone = totalChapters != null && chaptersProgress >= totalChapters
    val volumesDone = totalVolumes != null && volumesProgress >= totalVolumes
    return chaptersDone || volumesDone
}

private fun ReadingListEntry.incrementedChapters(): ReadingListEntry {
    val total = totalChapters
    val nextProgress = if (total == null) chaptersProgress + 1 else minOf(chaptersProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) ReadingListSection.COMPLETED else status
    return copy(chaptersProgress = nextProgress, status = nextStatus)
}

private fun ReadingListEntry.incrementedVolumes(): ReadingListEntry {
    val total = totalVolumes
    val nextProgress = if (total == null) volumesProgress + 1 else minOf(volumesProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) ReadingListSection.COMPLETED else status
    return copy(volumesProgress = nextProgress, status = nextStatus)
}
