package com.luum.michi.app.discover.ui.catalog

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRailItem

/**
 * The load/empty/content switch + poster grid shared by the genre page and the
 * Dashboard "View all" catalog. Both are the same server-ordered grid over a
 * `MediaCatalogFilter`; only the items and the optional header (the genre's
 * Anime/Manga switch) differ.
 */
@Composable
internal fun CatalogGridContent(
    holder: CatalogGridStateHolder,
    items: List<MediaRailItem>,
    strings: LanguageStrings,
    bottomPadding: Dp,
    resetKey: Any?,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (LazyGridScope.() -> Unit)? = null,
) {
    val hasContent = holder.pageLoaded
    LoadStateContainer(
        hasContent = hasContent,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (hasContent) holder.error?.let { strings.networkErrorMessage(it) } else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
    ) {
        DetailPosterGrid(
            items = items,
            onSelect = onOpenMedia,
            isLoading = holder.isLoading,
            isLoadingMore = holder.media.isLoadingMore,
            hasNextPage = holder.media.hasNextPage,
            loadedPage = holder.media.currentPage,
            onLoadMore = { holder.loadMore() },
            emptyLabel = strings.mediaDetailNoMediaLabel,
            resetKey = resetKey,
            bottomPadding = bottomPadding,
            header = header,
        )
    }
}
