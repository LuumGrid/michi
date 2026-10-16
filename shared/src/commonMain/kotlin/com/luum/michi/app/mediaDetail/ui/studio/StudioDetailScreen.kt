package com.luum.michi.app.mediaDetail.ui.studio

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.language.Strings

/**
 * Studio detail page: full-width identity hero (name + "Anime studio"/"Producer"
 * label + favourites) over a poster grid of the studio's works. The sort lives
 * in the holder and is edited from the shared `SortSheet` (Root toolbar action):
 * the server orders the grid, so a change re-queries from page 1. The grid pages
 * itself when its footer scrolls into view. Reached from a media information
 * panel studio chip via [com.luum.michi.app.root.state.State.openStudio].
 */
@Composable
internal fun StudioDetailScreen(
    holder: StudioDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    LoadStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (detail != null) holder.error?.let { strings.networkErrorMessage(it) } else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
    ) {
        if (detail != null) {
            StudioDetailGrid(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
                onOpenMedia = onOpenMedia,
            )
        }
    }
}

@Composable
private fun StudioDetailGrid(
    detail: StudioDetail,
    holder: StudioDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailPosterGrid(
        items = holder.mediaItems,
        hasNextPage = holder.hasNextPage,
        isLoading = holder.isLoading,
        isLoadingMore = holder.isLoadingMore,
        onLoadMore = { holder.loadMore() },
        onOpenMedia = onOpenMedia,
        emptyLabel = strings.mediaDetailNoMediaLabel,
        // Scroll rule: a sort change returns the grid to the top.
        resetKey = listOf(holder.sortOption, holder.sortOrder),
        modifier = modifier,
        bottomPadding = bottomPadding,
        header = {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                // Same type layout as the person heroes; a studio has no image.
                MediaDetailPersonHero(
                    name = detail.name,
                    // The animation flag reads as the line under the name.
                    nativeName = if (detail.isAnimeStudio) {
                        strings.mediaDetailAnimeStudioLabel
                    } else {
                        strings.mediaDetailProducerLabel
                    },
                    image = null,
                    favourites = detail.favourites,
                    strings = strings,
                    showPortrait = false,
                )
            }
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = strings.mediaDetailWorksTitle,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        },
    )
}
