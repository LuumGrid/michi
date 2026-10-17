package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
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
import com.luum.michi.app.core.media.domain.MediaGridItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * The one poster grid of a detail page that lists media (studio works, genre
 * works): a token-sized cell per [MediaGridItem] that pages itself when its
 * footer scrolls into view. [header] paints the page's own full-span items above
 * the grid (a hero, a title, chip rows) so each screen keeps its own header
 * without forking the grid; a page that wants no "Works" section header simply
 * does not add one. [resetKey] is the holder state that must send the grid back
 * to the top (a sort change, a catalog switch).
 */
@Composable
internal fun DetailPosterGrid(
    items: List<MediaGridItem>,
    hasNextPage: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    // Current page of the backing PagedList: the auto-paging effect keys on it
    // (not on items.size) so a fully-deduped page still advances the load, while
    // a failed page never does (no retry loop).
    loadedPage: Int,
    onLoadMore: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    emptyLabel: String,
    resetKey: Any?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    header: (LazyGridScope.() -> Unit)? = null,
) {
    val gridState = rememberResetGridState(resetKey)
    // Auto-pagination: the footer is the last item, so it scrolls into view
    // while the user keeps scrolling and loads the next page then. The flow is
    // distinct, so a failed page (footer still visible) never loops.
    if (hasNextPage) {
        LaunchedEffect(gridState, loadedPage) {
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey }
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { onLoadMore() }
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
            header?.invoke(this)
            if (items.isEmpty() && !isLoading) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = emptyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(items = items, key = { it.mediaId }) { item ->
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
                if (hasNextPage) {
                    item(key = LoadMoreKey, span = { GridItemSpan(maxLineSpan) }) {
                        LoadMoreFooter(isLoading = isLoadingMore)
                    }
                }
            }
        }
    }
}
