package com.luum.michi.app.mediaDetail.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.ui.components.GridCoverSizeProvider
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.PosterCard
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.favouritesBadge
import com.luum.michi.app.ui.components.scoreBadge
import com.luum.michi.app.ui.components.rememberResetGridState
import com.luum.michi.app.ui.language.Strings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Studio detail page: name hero + a poster grid. The sort lives in the holder
 * and is edited from the shared `SortSheet` (Root toolbar action): the server
 * orders the grid, so a change re-queries from page 1. The grid pages itself
 * when its footer scrolls into view. Reached from a media information panel
 * studio chip via [com.luum.michi.app.root.state.State.openStudio].
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
    // Scroll rule: a sort change returns the grid to the top (the key changes
    // when the holder's selection does).
    val gridState = rememberResetGridState(listOf(holder.sortOption, holder.sortOrder))
    // Auto-pagination: the footer is the last item, so it scrolls into view
    // while the user keeps scrolling and loads the next page then. The flow is
    // distinct, so a failed page (footer still visible) never loops.
    if (holder.hasNextPage) {
        LaunchedEffect(gridState, holder.mediaItems.size) {
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey }
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { holder.loadMore() }
        }
    }

    GridCoverSizeProvider { coverSize ->
        LazyVerticalGrid(
            // Cells are exactly a standard poster (the token the provider picked
            // from the available width); the leftover becomes even air instead of
            // a wide gutter inside every cell.
            columns = GridCells.FixedSize(coverSize.width),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            // No horizontal padding: SpaceEvenly owns all the side air, so the
            // gutter between columns equals the one at the edges.
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 24.dp + bottomPadding,
            ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                // Same type layout as the person heroes; a studio has no image.
                MediaDetailPersonHero(
                    name = detail.name,
                    // The anime-studio flag reads as the line under the name.
                    nativeName = strings.mediaDetailAnimeStudioLabel.takeIf { detail.isAnimeStudio },
                    image = null,
                    favourites = detail.favourites,
                    strings = strings,
                    showPortrait = false,
                )
            }
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = strings.mediaDetailMediaTitle,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (holder.mediaItems.isEmpty() && !holder.isLoading) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = strings.mediaDetailNoMediaLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(items = holder.mediaItems, key = { it.mediaId }) { item ->
                    PosterCard(
                        cover = item.cover,
                        title = item.title,
                        size = coverSize,
                        onClick = { onOpenMedia(item.mediaId) },
                        subtitle = item.genres.take(2).joinToString(" · ").ifBlank { null },
                        placeholderHex = item.paletteHex,
                        stats = listOfNotNull(
                            scoreBadge(item.averageScore),
                            favouritesBadge(item.favourites),
                        ),
                        // A grid cell lets the subtitle hug the title.
                        reserveTitleLines = false,
                    )
                }
                if (holder.hasNextPage) {
                    item(key = LoadMoreKey, span = { GridItemSpan(maxLineSpan) }) {
                        LoadMoreFooter(isLoading = holder.isLoadingMore)
                    }
                }
            }
        }
    }
}
