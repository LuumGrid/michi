package com.luum.michi.app.animation.presentation

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.*

internal enum class AnimationListSection {
    ALL,
    WATCHING,
    COMPLETED_TV,
    COMPLETED_MOVIE,
    COMPLETED_OVA,
    COMPLETED_ONA,
    COMPLETED_TV_SHORT,
    COMPLETED_SPECIAL,
    PAUSED,
    DROPPED,
    PLANNING,
    REWATCHING,
}

private val StatusSections = listOf(
    AnimationListSection.WATCHING,
    AnimationListSection.COMPLETED_TV,
    AnimationListSection.COMPLETED_MOVIE,
    AnimationListSection.COMPLETED_OVA,
    AnimationListSection.COMPLETED_ONA,
    AnimationListSection.COMPLETED_TV_SHORT,
    AnimationListSection.COMPLETED_SPECIAL,
    AnimationListSection.PAUSED,
    AnimationListSection.DROPPED,
    AnimationListSection.PLANNING,
    AnimationListSection.REWATCHING,
)

private data class AnimationListEntry(
    val id: Int,
    val title: String,
    val format: String,
    val status: AnimationListSection,
    val progress: Int,
    val totalEpisodes: Int?,
    val score: String,
    val nextEpisodeRelease: MediaReleaseDateTime?,
    val palette: List<Color>,
)

private val InitialEntries = listOf(
    AnimationListEntry(
        id = 1,
        title = "Frieren: Beyond Journey's End",
        format = "TV | Fantasy",
        status = AnimationListSection.WATCHING,
        progress = 18,
        totalEpisodes = 28,
        score = "9.2",
        nextEpisodeRelease = MediaReleaseDateTime(day = 24, month = 5, year = 2026, hour = 18, minute = 30),
        palette = listOf(Color(0xFF0F766E), Color(0xFF99F6E4)),
    ),
    AnimationListEntry(
        id = 2,
        title = "Delicious in Dungeon",
        format = "TV | Adventure",
        status = AnimationListSection.WATCHING,
        progress = 11,
        totalEpisodes = 24,
        score = "8.4",
        nextEpisodeRelease = MediaReleaseDateTime(day = 23, month = 5, year = 2026, hour = 21, minute = 0),
        palette = listOf(Color(0xFF854D0E), Color(0xFFFDE68A)),
    ),
    AnimationListEntry(
        id = 3,
        title = "Kaiju No. 8",
        format = "TV | Action",
        status = AnimationListSection.WATCHING,
        progress = 7,
        totalEpisodes = 12,
        score = "8.0",
        nextEpisodeRelease = MediaReleaseDateTime(day = 27, month = 5, year = 2026, hour = 17, minute = 45),
        palette = listOf(Color(0xFF1E293B), Color(0xFF38BDF8)),
    ),
    AnimationListEntry(
        id = 4,
        title = "A Sign of Affection",
        format = "TV | Romance",
        status = AnimationListSection.COMPLETED_TV,
        progress = 12,
        totalEpisodes = 12,
        score = "8.7",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF7C3AED), Color(0xFFC4B5FD)),
    ),
    AnimationListEntry(
        id = 5,
        title = "Spirited Away",
        format = "Movie | Fantasy",
        status = AnimationListSection.COMPLETED_MOVIE,
        progress = 1,
        totalEpisodes = 1,
        score = "9.0",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF0F172A), Color(0xFF38BDF8)),
    ),
    AnimationListEntry(
        id = 6,
        title = "Mobile Suit Gundam: Thunderbolt",
        format = "OVA | Sci-Fi",
        status = AnimationListSection.COMPLETED_OVA,
        progress = 4,
        totalEpisodes = 4,
        score = "8.1",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF1F2937), Color(0xFFD1D5DB)),
    ),
    AnimationListEntry(
        id = 7,
        title = "Cyberpunk: Edgerunners",
        format = "ONA | Action",
        status = AnimationListSection.COMPLETED_ONA,
        progress = 10,
        totalEpisodes = 10,
        score = "8.8",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF713F12), Color(0xFFFDE68A)),
    ),
    AnimationListEntry(
        id = 8,
        title = "Tsuredure Children",
        format = "TV Short | Comedy",
        status = AnimationListSection.COMPLETED_TV_SHORT,
        progress = 12,
        totalEpisodes = 12,
        score = "7.6",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF831843), Color(0xFFF9A8D4)),
    ),
    AnimationListEntry(
        id = 9,
        title = "Violet Evergarden Special",
        format = "Special | Drama",
        status = AnimationListSection.COMPLETED_SPECIAL,
        progress = 1,
        totalEpisodes = 1,
        score = "8.5",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF164E63), Color(0xFF67E8F9)),
    ),
    AnimationListEntry(
        id = 10,
        title = "Wind Breaker",
        format = "TV | Sports",
        status = AnimationListSection.PLANNING,
        progress = 0,
        totalEpisodes = 13,
        score = "-",
        nextEpisodeRelease = MediaReleaseDateTime(day = 10, month = 7, year = 2026, hour = 20, minute = 0),
        palette = listOf(Color(0xFF991B1B), Color(0xFFFCA5A5)),
    ),
    AnimationListEntry(
        id = 11,
        title = "Solo Leveling",
        format = "TV | Action",
        status = AnimationListSection.PAUSED,
        progress = 5,
        totalEpisodes = 12,
        score = "7.8",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF312E81), Color(0xFFA5B4FC)),
    ),
    AnimationListEntry(
        id = 12,
        title = "The Detective Is Already Dead",
        format = "TV | Mystery",
        status = AnimationListSection.DROPPED,
        progress = 3,
        totalEpisodes = 12,
        score = "5.5",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF3F3F46), Color(0xFFA1A1AA)),
    ),
    AnimationListEntry(
        id = 13,
        title = "Hunter x Hunter (2011)",
        format = "TV | Adventure",
        status = AnimationListSection.REWATCHING,
        progress = 45,
        totalEpisodes = 148,
        score = "9.5",
        nextEpisodeRelease = null,
        palette = listOf(Color(0xFF14532D), Color(0xFF4ADE80)),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimationScreen(
    selectedSection: AnimationListSection,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val strings = LanguageProvider.strings
    val entries = remember { mutableStateListOf<AnimationListEntry>().also { it.addAll(InitialEntries) } }
    var editingEntry by remember { mutableStateOf<AnimationListEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectedSection == AnimationListSection.ALL) {
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
                            key = AnimationListEntry::id,
                        ) { entry ->
                            AnimationListCard(
                                entry = entry,
                                onOpen = {},
                                onEdit = { editingEntry = entry },
                                onIncrementProgress = { entries.increment(entry) },
                            )
                        }
                    }
                }
            } else {
                val visibleEntries = entries.filter { it.status == selectedSection }
                item {
                    PlatformSectionHeader(
                        title = selectedSection.label(strings),
                        count = visibleEntries.size,
                    )
                }
                items(
                    items = visibleEntries,
                    key = AnimationListEntry::id,
                ) { entry ->
                    AnimationListCard(
                        entry = entry,
                        onOpen = {},
                        onEdit = { editingEntry = entry },
                        onIncrementProgress = { entries.increment(entry) },
                    )
                }
            }
        }

        editingEntry?.let { entry ->
            AnimationEditSheet(
                entry = entry,
                onDismiss = { editingEntry = null },
            )
        }
    }
}

@Composable
private fun AnimationEditSheet(
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
    val statusOptions = remember {
        listOf(
            AnimationListSection.WATCHING,
            AnimationListSection.PLANNING,
            AnimationListSection.COMPLETED_TV,
            AnimationListSection.DROPPED,
            AnimationListSection.PAUSED,
            AnimationListSection.REWATCHING,
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
                    PlatformScoreField(
                        score = score,
                        onScoreChange = { score = it },
                    )
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

                item {
                    PlatformDateField(label = strings.startedLabel, value = "2 Jul 2024")
                }

                item {
                    PlatformDateField(label = strings.completedLabel, value = if (selectedStatus.isCompleted) strings.todayLabel else "")
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
internal fun AnimationSectionPlatformChips(
    selected: AnimationListSection,
    onSelect: (AnimationListSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    val sections = remember { AnimationListSection.entries }

    PlatformChips(
        items = sections,
        selectedItem = selected,
        onSelect = onSelect,
        label = { section ->
            val count = if (section == AnimationListSection.ALL) {
                InitialEntries.size
            } else {
                InitialEntries.count { it.status == section }
            }
            "${section.label(strings)} $count"
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun AnimationListCard(
    entry: AnimationListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementProgress: () -> Unit,
) {
    PlatformMediaListCard(
        title = entry.title,
        subtitle = entry.format,
        score = entry.score,
        primaryProgressLabel = entry.progressLabel(),
        primaryProgressRatio = entry.progressRatio(),
        primaryIncrementLabel = "+1 EP",
        primaryIncrementValueLabel = entry.progressLabel(),
        primaryIncrementEnabled = entry.canIncrement(),
        palette = entry.palette,
        icon = PlatformIcons.Animation,
        isComplete = !entry.canIncrement(),
        releaseLabel = entry.releaseLabel(LanguageProvider.strings),
        behindLabel = entry.behindLabel(LanguageProvider.strings),
        fallbackStatusLabel = entry.status.label(LanguageProvider.strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementProgress,
    )
}

private fun AnimationListEntry.progressLabel(): String {
    val total = totalEpisodes?.toString() ?: "?"
    return "$progress / $total"
}

private fun AnimationListEntry.progressRatio(): Float {
    val total = totalEpisodes ?: return 0f
    return if (total == 0) 0f else (progress.toFloat() / total).coerceIn(0f, 1f)
}

private fun AnimationListEntry.canIncrement(): Boolean {
    val total = totalEpisodes
    return total == null || progress < total
}

private fun AnimationListEntry.releaseLabel(strings: LanguageStrings): String? {
    return nextEpisodeRelease?.let { release ->
        strings.nextEpisodeReleaseLabel(episodeNumber = progress + 1, releaseDateTime = release)
    }
}

private fun AnimationListEntry.behindLabel(strings: LanguageStrings): String? {
    val total = totalEpisodes
    val behind = if (total != null) total - progress else 0
    return if (status == AnimationListSection.WATCHING && behind > 0) {
        strings.episodesBehind(behind)
    } else {
        null
    }
}

private fun AnimationListEntry.incremented(): AnimationListEntry {
    val total = totalEpisodes
    val nextProgress = if (total == null) progress + 1 else minOf(progress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) completedSectionForFormat() else status
    return copy(progress = nextProgress, status = nextStatus)
}

private val AnimationListSection.isCompleted: Boolean
    get() = when (this) {
        AnimationListSection.COMPLETED_TV,
        AnimationListSection.COMPLETED_MOVIE,
        AnimationListSection.COMPLETED_OVA,
        AnimationListSection.COMPLETED_ONA,
        AnimationListSection.COMPLETED_TV_SHORT,
        AnimationListSection.COMPLETED_SPECIAL -> true
        AnimationListSection.ALL,
        AnimationListSection.WATCHING,
        AnimationListSection.PAUSED,
        AnimationListSection.DROPPED,
        AnimationListSection.PLANNING,
        AnimationListSection.REWATCHING -> false
    }

private fun AnimationListSection.label(strings: LanguageStrings): String = when (this) {
    AnimationListSection.ALL -> strings.sectionAll
    AnimationListSection.WATCHING -> strings.sectionWatching
    AnimationListSection.COMPLETED_TV -> strings.sectionCompletedTv
    AnimationListSection.COMPLETED_MOVIE -> strings.sectionCompletedMovie
    AnimationListSection.COMPLETED_OVA -> strings.sectionCompletedOva
    AnimationListSection.COMPLETED_ONA -> strings.sectionCompletedOna
    AnimationListSection.COMPLETED_TV_SHORT -> strings.sectionCompletedTvShort
    AnimationListSection.COMPLETED_SPECIAL -> strings.sectionCompletedSpecial
    AnimationListSection.PAUSED -> strings.sectionPaused
    AnimationListSection.DROPPED -> strings.sectionDropped
    AnimationListSection.PLANNING -> strings.sectionPlanning
    AnimationListSection.REWATCHING -> strings.sectionRewatching
}

private fun AnimationListSection.modalLabel(strings: LanguageStrings): String {
    return when (this) {
        AnimationListSection.COMPLETED_TV,
        AnimationListSection.COMPLETED_MOVIE,
        AnimationListSection.COMPLETED_OVA,
        AnimationListSection.COMPLETED_ONA,
        AnimationListSection.COMPLETED_TV_SHORT,
        AnimationListSection.COMPLETED_SPECIAL -> strings.sectionCompleted
        else -> label(strings)
    }
}

private fun AnimationListEntry.completedSectionForFormat(): AnimationListSection {
    return when {
        format.startsWith("Movie") -> AnimationListSection.COMPLETED_MOVIE
        format.startsWith("OVA") -> AnimationListSection.COMPLETED_OVA
        format.startsWith("ONA") -> AnimationListSection.COMPLETED_ONA
        format.startsWith("TV Short") -> AnimationListSection.COMPLETED_TV_SHORT
        format.startsWith("Special") -> AnimationListSection.COMPLETED_SPECIAL
        else -> AnimationListSection.COMPLETED_TV
    }
}

private fun MutableList<AnimationListEntry>.increment(entry: AnimationListEntry) {
    val index = indexOfFirst { it.id == entry.id }
    if (index != -1) {
        this[index] = this[index].incremented()
    }
}
