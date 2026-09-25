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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.animeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.anime.model.releaseLabel
import com.luum.michi.app.mediaList.domain.common.mediaListFormatOptions
import com.luum.michi.app.mediaList.domain.common.mediaListGenres
import com.luum.michi.app.mediaList.domain.common.mediaListSeasonOptions
import com.luum.michi.app.mediaList.domain.common.mediaListYears
import com.luum.michi.app.mediaList.ui.anime.components.AnimeListEntryContent
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.common.ANY_SEASON_ID
import com.luum.michi.app.mediaList.ui.common.MediaListFilterSheet
import com.luum.michi.app.mediaList.ui.common.MediaListSectionRail
import com.luum.michi.app.mediaList.ui.common.MediaListSectionTab
import com.luum.michi.app.mediaList.ui.common.MediaListSortSheet
import com.luum.michi.app.mediaList.ui.common.toggled
import com.luum.michi.app.mediaList.ui.common.yearFilterTabs
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
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
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    // In-context search: filters the current section by title (bestTitle
    // only — v1 limitation). Empty query shows the section untouched.
    query: String = "",
    // List tools (toolbar filter/sort): Root owns the open flags so the
    // toolbar back affordance and system back close the sheets.
    isFilterOpen: Boolean = false,
    onDismissFilter: () -> Unit = {},
    isSortOpen: Boolean = false,
    onDismissSort: () -> Unit = {},
    // Mirrors the Settings persist toggle: when on, sort changes are
    // remembered per tab across restarts (filters never persist).
    sortPersist: Boolean = false,
    // Mirrors the Settings split toggle: merged COMPLETED when off,
    // per-format sections when on.
    splitCompleted: Boolean = true,
    // Global 18+ setting (inverted): hidden entries never reach the
    // list, but rail counts stay raw (see holder.countInSection).
    hideAdult: Boolean = false,
) {
    val sections = listOf(AnimeListSection.ALL) + animeStatusSections(splitCompleted)
    // Toggling split can strand the selection on a gone section (e.g.
    // COMPLETED_TV with split off): fall back to ALL without churning state.
    val effectiveSelected = if (selected in sections) selected else AnimeListSection.ALL
    val tabs = sections.map { section ->
        MediaListSectionTab(
            value = section,
            label = section.label(strings),
            count = holder.countInSection(section),
        )
    }
    val trimmedQuery = query.trim()
    val entries = holder.entriesInSection(effectiveSelected, hideAdult).let { sectionEntries ->
        if (trimmedQuery.isEmpty()) {
            sectionEntries
        } else {
            sectionEntries.filter { entry ->
                entry.titles.any { it.contains(trimmedQuery, ignoreCase = true) }
            }
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        MediaListSectionRail(
            tabs = tabs,
            selected = effectiveSelected,
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
                // No match under an active query reads as no-results, not as
                // an empty section (different message, same panel).
                val noMatch = trimmedQuery.isNotEmpty()
                MessagePanel(
                    title = if (noMatch) strings.searchNoResultsLabel else strings.listsEmptyLabel,
                    message = null,
                    icon = if (noMatch) AppIcons.Search else AppIcons.Anime,
                    actionLabel = null,
                    onAction = {},
                )
            }
            else -> {
                PullRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                ) {
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
                        val seasonYear = entry.season?.let { season ->
                            val year = entry.seasonYear?.toString()
                            if (year != null) "${season.label(strings)} $year" else season.label(strings)
                        } ?: entry.seasonYear?.toString()
                        val meta = listOfNotNull(
                            entry.format.label(),
                            seasonYear,
                            entry.mediaStatus?.label(strings, isManga = false),
                        ).joinToString(" · ")
                        MediaCoverCard(
                            coverUrl = entry.coverUrl,
                            title = entry.title,
                            subtitle = meta,
                            // TODO: open the quick-edit sheet (mini-step B);
                            // cover/title taps will navigate to media detail.
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
    if (isFilterOpen) {
        // Accordion decade: screen-local, auto-expands the decade holding
        // the current year filter. Root never sees it (single sheet, so
        // back always closes the filter sheet — no ordering involved).
        var expandedDecade by remember {
            mutableStateOf(holder.filterYear?.let { it / 10 * 10 })
        }
        val yearTabs = yearFilterTabs(
            entries = holder.entries,
            years = mediaListYears(),
            anyLabel = strings.exploreAnyYearLabel,
        )
        MediaListFilterSheet(
            seasonOptions = mediaListSeasonOptions(strings),
            selectedSeasonId = holder.filterSeason?.name ?: ANY_SEASON_ID,
            onSelectSeason = { id ->
                holder.updateListFilters(
                    season = if (id == ANY_SEASON_ID) null else parseMediaSeason(id),
                    genres = holder.filterGenres,
                    formats = holder.filterFormats,
                    year = holder.filterYear,
                )
            },
            yearTabs = yearTabs,
            selectedYear = holder.filterYear,
            onSelectYear = { year ->
                holder.updateListFilters(
                    season = holder.filterSeason,
                    genres = holder.filterGenres,
                    formats = holder.filterFormats,
                    year = year,
                )
            },
            expandedDecade = expandedDecade,
            onToggleDecade = { expandedDecade = if (it == expandedDecade) null else it },
            genres = mediaListGenres(),
            selectedGenres = holder.filterGenres,
            onToggleGenre = { genre ->
                holder.updateListFilters(
                    season = holder.filterSeason,
                    genres = holder.filterGenres.toggled(genre),
                    formats = holder.filterFormats,
                    year = holder.filterYear,
                )
            },
            formatOptions = mediaListFormatOptions(isAnimeTab = true),
            selectedFormatIds = holder.filterFormats,
            onToggleFormat = { formatId ->
                holder.updateListFilters(
                    season = holder.filterSeason,
                    genres = holder.filterGenres,
                    formats = holder.filterFormats.toggled(formatId),
                    year = holder.filterYear,
                )
            },
            onReset = {
                expandedDecade = null
                holder.updateListFilters(
                    season = null,
                    genres = emptyList(),
                    formats = emptyList(),
                    year = null,
                )
            },
            onDismiss = {
                expandedDecade = null
                onDismissFilter()
            },
        )
    }
    if (isSortOpen) {
        MediaListSortSheet(
            options = UserListSort.entries,
            selected = holder.currentSortOption,
            onSelect = { option ->
                holder.updateSort(option, holder.currentSortOrder, persist = sortPersist)
            },
            order = holder.currentSortOrder,
            onSelectOrder = { order ->
                holder.updateSort(holder.currentSortOption, order, persist = sortPersist)
            },
            onDismiss = onDismissSort,
        )
    }
}
