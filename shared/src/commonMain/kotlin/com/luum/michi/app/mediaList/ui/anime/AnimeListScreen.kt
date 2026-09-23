package com.luum.michi.app.mediaList.ui.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.label
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.anime.model.releaseLabel
import com.luum.michi.app.mediaList.ui.anime.components.AnimeListEntryContent
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.common.MediaListSectionRail
import com.luum.michi.app.mediaList.ui.common.MediaListSectionTab
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Anime list: section rail + entry cards. States reuse the app-wide
 * vocabulary (spinner, [MessagePanel] with retry, empty label). Search,
 * filters and sort land in later steps; the holder already supports them.
 */
@Composable
internal fun AnimeListScreen(
    holder: AnimeListStateHolder,
    selected: AnimeListSection,
    onSelectSection: (AnimeListSection) -> Unit,
    onRetry: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last item clears the floating
    // TabBar with the same 16dp rhythm the first item keeps under the rail.
    bottomPadding: Dp = 0.dp,
) {
    val sections = listOf(AnimeListSection.ALL) + AnimeStatusSections
    val tabs = sections.map { section ->
        MediaListSectionTab(
            value = section,
            label = section.label(strings),
            count = holder.countInSection(section),
        )
    }
    val entries = holder.entriesInSection(selected)
    Column(modifier = modifier.fillMaxSize()) {
        MediaListSectionRail(
            tabs = tabs,
            selected = selected,
            onSelect = onSelectSection,
        )
        when {
            holder.isLoading && entries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.listsLoadingLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            holder.error != null && entries.isEmpty() -> {
                MessagePanel(
                    title = strings.listsErrorLabel,
                    message = holder.error?.let { strings.networkErrorMessage(it) },
                    icon = Icons.Filled.Warning,
                    actionLabel = strings.retryAction,
                    onAction = onRetry,
                )
            }
            entries.isEmpty() -> {
                MessagePanel(
                    title = strings.listsEmptyLabel,
                    message = null,
                    icon = AppIcons.Anime,
                    actionLabel = null,
                    onAction = {},
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp + bottomPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = entries,
                        key = { it.id },
                    ) { entry ->
                        val release = entry.releaseLabel(strings)
                        val meta = if (release != null) {
                            "${entry.format.label()} · $release"
                        } else {
                            entry.format.label()
                        }
                        MediaCoverCard(
                            coverUrl = entry.coverUrl,
                            title = entry.title,
                            subtitle = meta,
                            // TODO: navigate to media detail.
                            onClick = {},
                        ) {
                            AnimeListEntryContent(
                                entry = entry,
                                onIncrement = { holder.incrementProgress(entry) },
                                strings = strings,
                            )
                        }
                    }
                }
            }
        }
    }
}
