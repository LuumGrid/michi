package com.luum.michi.app.mediaDetail.ui.studio

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.studioWorkSections
import com.luum.michi.app.mediaDetail.ui.components.DetailLoadState
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.PosterGridSection
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.toPosterItem
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
    DetailLoadState(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        error = holder.error,
        actionError = holder.actionError,
        onRetry = onRetry,
        onDismissActionError = { holder.clearActionError() },
        strings = strings,
        modifier = modifier,
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
    val posterItems = remember(holder.media.items) { holder.media.items.map { it.toPosterItem() } }
    val noYearLabel = strings.mediaDetailStudioWorksNoYearLabel
    val workSections = remember(holder.media.items, holder.sortOption, noYearLabel) {
        holder.media.items.studioWorkSections(holder.sortOption, noYearLabel)
    }
    val posterSections = remember(workSections) {
        workSections?.map { section ->
            PosterGridSection(
                key = section.key,
                title = section.title,
                items = section.items.map { it.toPosterItem() },
            )
        }
    }
    DetailPosterGrid(
        items = posterItems,
        onSelect = onOpenMedia,
        isLoading = holder.isLoading,
        isLoadingMore = holder.media.isLoadingMore,
        hasNextPage = holder.media.hasNextPage,
        loadedPage = holder.media.currentPage,
        onLoadMore = { holder.loadMore() },
        emptyLabel = strings.mediaDetailNoMediaLabel,
        modifier = modifier,
        // Scroll rule: a sort change returns the grid to the top.
        resetKey = listOf(holder.sortOption, holder.sortOrder),
        bottomPadding = bottomPadding,
        sections = posterSections,
        header = {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                // Same type layout as the person heroes; a studio has no image.
                MediaDetailPersonHero(
                    name = detail.name,
                    // The animation flag reads as the line under the name.
                    secondaryLine = if (detail.isAnimeStudio) {
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
            if (workSections == null) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = strings.mediaDetailWorksTitle,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        },
    )
}
