package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.FilterOption
import com.luum.michi.app.core.media.domain.MediaGenres
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.media.domain.parseMediaSeason
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import com.luum.michi.app.mediaList.domain.common.mediaListSeasonOptions
import com.luum.michi.app.mediaList.domain.common.mediaListYears
import com.luum.michi.app.mediaList.ui.common.state.MediaListHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.components.RefreshErrorRow
import com.luum.michi.app.ui.components.SortSheet
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Scroll reset key (app-wide scroll rule): changing what is shown — section,
 * sort or a filter — returns the list to the top. The search query is
 * deliberately excluded: typing must never jump. Pure, so the key composition
 * is testable without composition.
 */
internal fun mediaListResetKey(
    selected: Any?,
    sortOption: UserListSort,
    sortOrder: UserListOrder,
    season: MediaSeason?,
    genres: List<String>,
    formats: List<String>,
    year: Int?,
): List<Any?> = listOf(selected, sortOption, sortOrder, season, genres, formats, year)

/**
 * The one list screen: section rail + entry cards, shared by the anime and
 * manga lists. Everything genuinely per-list arrives as a parameter or slot
 * ([allSection], [statusSections], [sectionLabel], [emptyIcon], [meta],
 * [formatOptions], [cardContent]); the rest (states, search, scroll, filters,
 * sort sheet) lives here. States reuse the app-wide vocabulary (spinner,
 * [MessagePanel] with retry, empty label).
 */
@Composable
internal fun <T : SortableMediaListEntry, S> MediaListScreenScaffold(
    holder: MediaListHolder<T, S>,
    selected: S,
    onSelectSection: (S) -> Unit,
    onRetry: () -> Unit,
    allSection: S,
    statusSections: (Boolean) -> List<S>,
    sectionLabel: (S, LanguageStrings) -> String,
    emptyIcon: ImageVector,
    meta: (T, LanguageStrings) -> String,
    formatOptions: (LanguageStrings) -> List<FilterOption>,
    cardContent: @Composable ColumnScope.(T) -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last item clears the floating
    // TabBar with the same 16dp rhythm the first item keeps under the rail.
    bottomPadding: Dp = 0.dp,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    // In-context search: filters the current section by title (all variants).
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
    // Global 18+ setting (inverted): hidden entries never reach the list, but
    // rail counts stay raw (see holder.countInSection).
    hideAdult: Boolean = false,
    // Card tap opens media detail, pencil opens the entry editor.
    onOpenDetail: (Int) -> Unit = {},
    onEditEntry: (Int) -> Unit = {},
) {
    val sections = listOf(allSection) + statusSections(splitCompleted)
    // Toggling split can strand the selection on a gone section (e.g.
    // COMPLETED_TV with split off): fall back to ALL without churning state.
    val effectiveSelected = if (selected in sections) selected else allSection
    // Scroll rule: changing the section, the sort or a filter returns to the
    // top; the search query is not part of the key, so typing never jumps; and
    // refresh, sheet open/close or coming back from a detail keeps the position.
    val listState = rememberResetListState(
        mediaListResetKey(
            selected = effectiveSelected,
            sortOption = holder.currentSortOption,
            sortOrder = holder.currentSortOrder,
            season = holder.filterSeason,
            genres = holder.filterGenres,
            formats = holder.filterFormats,
            year = holder.filterYear,
        ),
    )
    val tabs = sections.map { section ->
        ChipTab(
            value = section,
            label = sectionLabel(section, strings),
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
    // "Nothing loaded" is about the loaded list, not the filtered view: a query
    // (or a sheet filter) that empties the view is not a load failure.
    val hasLoadedEntries = holder.entries.isNotEmpty()
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    Column(modifier = modifier.fillMaxSize()) {
        ChipRail(
            tabs = tabs,
            selected = effectiveSelected,
            onSelect = onSelectSection,
        )
        // With content already loaded, a failed refresh is a compact retry line
        // above the list, never a full-page error that hides the entries.
        if (errorMessage != null && hasLoadedEntries) {
            RefreshErrorRow(
                message = errorMessage,
                retryLabel = strings.retryAction,
                onRetry = onRetry,
            )
        }
        when {
            holder.isLoading && !hasLoadedEntries -> {
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
            errorMessage != null && !hasLoadedEntries -> {
                MessagePanel(
                    title = strings.listsErrorLabel,
                    message = errorMessage,
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
                    icon = if (noMatch) AppIcons.Search else emptyIcon,
                    actionLabel = null,
                    onAction = {},
                )
            }
            else -> {
                holder.actionError?.let { error ->
                    RefreshErrorRow(
                        message = strings.networkErrorMessage(error),
                        dismissLabel = strings.dismissAction,
                        onDismiss = { holder.clearActionError() },
                    )
                }
                PullRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                ) {
                    LazyColumn(
                        state = listState,
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
                            MediaCoverCard(
                                cover = entry.cover,
                                title = entry.title,
                                subtitle = meta(entry, strings),
                                onClick = { onOpenDetail(entry.id) },
                                titleTrailing = {
                                    IconButton(onClick = { onEditEntry(entry.id) }) {
                                        Icon(
                                            imageVector = AppIcons.Edit,
                                            contentDescription = strings.editEntryAction,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                content = { cardContent(entry) },
                            )
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
            genres = MediaGenres,
            selectedGenres = holder.filterGenres,
            onToggleGenre = { genre ->
                holder.updateListFilters(
                    season = holder.filterSeason,
                    genres = holder.filterGenres.toggled(genre),
                    formats = holder.filterFormats,
                    year = holder.filterYear,
                )
            },
            formatOptions = formatOptions(strings),
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
        SortSheet(
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
