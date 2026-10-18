package com.luum.michi.app.discover.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.discover.domain.model.CatalogSortOptions
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.domain.model.filterLabel
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.SortSheet
import com.luum.michi.app.ui.components.favouritesBadge
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.components.scoreBadge
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Explore: the DISCOVER search results. It is the tab's content while a query
 * has been submitted. After a submit the search field collapses and the toolbar
 * (results mode) carries the title, filter/sort and the magnifier, so this
 * screen only paints the category chip row and the results. The search itself
 * never fires while typing — only on submit (see `ExploreStateHolder.submitSearch`),
 * to respect AniList's request budget. Filters edit a local draft and apply once
 * when the sheet closes.
 *
 * Results reuse the shared presentation: media and people render through
 * [DetailPosterGrid] (the same grid as the genre page and the "View all" lists),
 * studios are settings-style rows (a studio has no image). Pagination is
 * automatic when the footer scrolls into view; a new submit or a chip change
 * returns the list to the top.
 */
@Composable
internal fun ExploreScreen(
    holder: ExploreStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    hasSession: Boolean,
    isFilterOpen: Boolean,
    onDismissFilter: () -> Unit,
    isSortOpen: Boolean,
    onDismissSort: () -> Unit,
    onRetry: () -> Unit,
    onOpenResult: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        val isMediaCategory = holder.category == ExploreCategory.ANIME ||
            holder.category == ExploreCategory.MANGA
        val chips = remember(strings) {
            ExploreCategory.entries.map { ChipTab(value = it, label = it.filterLabel(strings)) }
        }
        ChipRail(
            tabs = chips,
            selected = holder.category,
            onSelect = { holder.changeCategory(it) },
        )
        val hasContent = holder.results.isNotEmpty()
        LoadStateContainer(
            hasContent = hasContent,
            isLoading = holder.isLoading,
            errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
            loadingLabel = strings.discoverLoadingLabel,
            waitingLabel = strings.networkThrottleWaitingLabel,
            errorTitle = strings.discoverErrorLabel,
            retryLabel = strings.retryAction,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
            refreshError = if (hasContent) holder.error?.let { strings.networkErrorMessage(it) } else null,
            actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holder.clearActionError() },
            empty = {
                MessagePanel(
                    title = strings.exploreNoResultsLabel,
                    message = null,
                    icon = AppIcons.Search,
                )
            },
        ) {
            // A new submit or a category change returns the list to the top.
            val resetKey = listOf(holder.category, holder.query)
            if (holder.category == ExploreCategory.STUDIOS) {
                StudioRows(
                    items = holder.results,
                    isLoadingMore = holder.isLoadingMore,
                    hasNextPage = holder.hasNextPage,
                    loadedPage = holder.currentPage,
                    onLoadMore = { holder.loadMore() },
                    onSelect = onOpenResult,
                    resetKey = resetKey,
                    bottomPadding = bottomPadding,
                    animeStudioLabel = strings.mediaDetailAnimeStudioLabel,
                    producerLabel = strings.mediaDetailProducerLabel,
                )
            } else {
                // People repeat the chip as a subtitle, so only media keep meta.
                val showMeta = isMediaCategory
                val items = remember(holder.results, showMeta) {
                    holder.results.map { it.toRailItem(showMeta) }
                }
                DetailPosterGrid(
                    items = items,
                    onSelect = onOpenResult,
                    isLoading = holder.isLoading,
                    isLoadingMore = holder.isLoadingMore,
                    hasNextPage = holder.hasNextPage,
                    loadedPage = holder.currentPage,
                    onLoadMore = { holder.loadMore() },
                    emptyLabel = null,
                    resetKey = resetKey,
                    bottomPadding = bottomPadding,
                )
            }
        }

        if (isFilterOpen) {
            // The draft is local: taps never hit the network. It commits once
            // when the sheet closes (X, scrim or system back all leave this
            // branch), and a no-op when it equals what is already applied.
            var draft by remember { mutableStateOf(holder.appliedFilters) }
            DisposableEffect(Unit) {
                onDispose { holder.applyFilters(draft) }
            }
            ExploreFilterSheet(
                draft = draft,
                onDraftChange = { draft = it },
                onReset = { draft = ExploreFilters.Empty },
                showSeason = holder.category == ExploreCategory.ANIME,
                hasSession = hasSession,
                hideAdult = holder.hideAdult,
                onDismiss = onDismissFilter,
            )
        }
        if (isSortOpen) {
            SortSheet(
                options = CatalogSortOptions,
                selected = holder.currentSortOption,
                onSelect = { holder.updateSort(it, holder.currentSortOrder) },
                order = holder.currentSortOrder,
                onSelectOrder = { holder.updateSort(holder.currentSortOption, it) },
                onDismiss = onDismissSort,
            )
        }
    }
}

/**
 * Studio results: settings-style rows (name + "Anime studio"/"Producer"). A
 * studio search is short, so all loaded rows live in one [OptionGroup] item so
 * the frame stays continuous; the auto-paging footer is its own item.
 */
@Composable
private fun StudioRows(
    items: List<ExploreResult>,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    loadedPage: Int,
    onLoadMore: () -> Unit,
    onSelect: (Int) -> Unit,
    resetKey: Any?,
    bottomPadding: Dp,
    animeStudioLabel: String,
    producerLabel: String,
) {
    val listState = rememberResetListState(resetKey)
    LoadMoreEffect(
        state = listState,
        enabled = hasNextPage,
        loadedPage = loadedPage,
        onLoadMore = onLoadMore,
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp + bottomPadding,
        ),
    ) {
        item(key = "studio-rows") {
            OptionGroup(title = null) {
                items.forEachIndexed { index, item ->
                    SettingsRow(
                        label = item.title,
                        value = if (item.isAnimationStudio) animeStudioLabel else producerLabel,
                        onClick = { onSelect(item.id) },
                        divider = index > 0,
                    )
                }
            }
        }
        if (hasNextPage) {
            item(key = LoadMoreKey) { LoadMoreFooter(isLoading = isLoadingMore) }
        }
    }
}

/** Catalog line as subtitle for media; people carry none (the chip already says it). */
private fun ExploreResult.toRailItem(showMeta: Boolean): MediaRailItem = MediaRailItem(
    id = id,
    title = title,
    image = cover,
    subtitle = meta.takeIf { showMeta && it.isNotBlank() },
    placeholderHex = paletteHex,
    stats = listOfNotNull(scoreBadge(averageScore), favouritesBadge(favourites)),
)
