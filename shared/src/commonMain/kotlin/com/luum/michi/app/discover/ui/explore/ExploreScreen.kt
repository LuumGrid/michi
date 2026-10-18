package com.luum.michi.app.discover.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.domain.model.filterLabel
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.ui.components.ActionRow
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.favouritesBadge
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.components.scoreBadge
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Explore: the DISCOVER search results. It is the tab's content while a query
 * has been submitted (before that the Dashboard shows behind the search field).
 * A category chip row switches the catalog; the search itself never fires while
 * typing — only on submit (see `ExploreStateHolder.submitSearch`), to respect
 * AniList's request budget.
 *
 * Results reuse the shared presentation: media and people render through
 * [DetailPosterGrid] (the same grid as the genre page and the "View all" lists),
 * studios are plain text rows (a studio has no image). Pagination is automatic
 * when the footer scrolls into view; a new submit or a chip change returns the
 * list to the top.
 */
@Composable
internal fun ExploreScreen(
    holder: ExploreStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenResult: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
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
                )
            } else {
                // People repeat the chip as a subtitle, so only media keep meta.
                val showMeta = holder.category == ExploreCategory.ANIME ||
                    holder.category == ExploreCategory.MANGA
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
    }
}

/**
 * A studio has no image on AniList (see `ExploreRepositoryImpl`), so its results
 * are plain text rows instead of a cover grid.
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
        itemsIndexed(
            items = items,
            key = { index, item -> "${item.id}#$index" },
        ) { index, item ->
            ActionRow(
                label = item.title,
                onClick = { onSelect(item.id) },
                divider = index > 0,
            )
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
