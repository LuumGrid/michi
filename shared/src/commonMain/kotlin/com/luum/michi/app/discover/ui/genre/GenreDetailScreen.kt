package com.luum.michi.app.discover.ui.genre

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.discover.ui.genre.state.GenreDetailStateHolder
import com.luum.michi.app.ui.components.CatalogChipRail
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.toPosterItem
import com.luum.michi.app.ui.language.Strings

/**
 * Genre page: an Anime/Manga chip row that switches the catalog over a poster
 * grid of that catalog's works. A genre is not an entity with a hero, so its
 * name lives in the toolbar (like a "View all" list), not in the content; the
 * chips are the grid's only header. The sort lives in the holder and is edited
 * from the shared `SortSheet` (Root toolbar action): the server orders the grid,
 * so a change re-queries from page 1. The grid pages itself when its footer
 * scrolls into view. Reached from a media genre chip via
 * [com.luum.michi.app.root.state.State.openGenre].
 */
@Composable
internal fun GenreDetailScreen(
    holder: GenreDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadStateContainer(
        hasContent = holder.pageLoaded,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (holder.pageLoaded) holder.error?.let { strings.networkErrorMessage(it) } else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
    ) {
        val posterItems = remember(holder.media.items) { holder.media.items.map { it.toPosterItem() } }
        DetailPosterGrid(
            items = posterItems,
            onSelect = onOpenMedia,
            isLoading = holder.isLoading,
            isLoadingMore = holder.media.isLoadingMore,
            hasNextPage = holder.media.hasNextPage,
            loadedPage = holder.media.currentPage,
            onLoadMore = { holder.loadMore() },
            emptyLabel = strings.mediaDetailNoMediaLabel,
            // Scroll rule: a catalog or sort change returns the grid to the top.
            resetKey = listOf(holder.type, holder.sortOption, holder.sortOrder),
            bottomPadding = bottomPadding,
            header = {
                item(key = "type", span = { GridItemSpan(maxLineSpan) }) {
                    // The catalog switch is the grid's only header: the genre
                    // name already reads in the toolbar, so no title item and no
                    // "Works" line.
                    CatalogChipRail(
                        anime = MediaType.ANIME,
                        manga = MediaType.MANGA,
                        selected = holder.type,
                        onSelect = { holder.changeType(it) },
                        strings = strings,
                    )
                }
            },
        )
    }
}
