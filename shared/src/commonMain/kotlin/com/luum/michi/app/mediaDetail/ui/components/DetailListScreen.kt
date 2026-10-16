package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.GridCoverSizeProvider
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.PosterCard
import com.luum.michi.app.ui.components.RefreshErrorRow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** How a [DetailListScreen] lays its items out. */
internal enum class DetailListLayout {
    /** Full-width cover rows (media: relations, recommendations, works). */
    ROWS,

    /** Portrait cards in a grid (people: characters, staff). */
    GRID,
}

/**
 * Full-screen "View all" list for a detail section. Stateless: the caller
 * reads its holder and passes the items + paging. Auto-loads the next page
 * when the footer scrolls into view, so the user just keeps scrolling. The
 * section title lives in the toolbar, not here.
 */
@Composable
internal fun DetailListScreen(
    items: List<MediaRailItem>,
    layout: DetailListLayout,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    // Page-load failure of the parent holder, shown as a compact dismissible row.
    actionError: String? = null,
    dismissLabel: String? = null,
    onDismissActionError: (() -> Unit)? = null,
) {
    val paged = hasMore && (onLoadMore != null || isLoadingMore)
    Column(modifier = modifier.fillMaxSize()) {
        if (actionError != null) {
            RefreshErrorRow(
                message = actionError,
                dismissLabel = dismissLabel,
                onDismiss = onDismissActionError,
            )
        }
        when (layout) {
            DetailListLayout.ROWS -> RowsList(
                items = items,
                onSelect = onSelect,
                bottomPadding = bottomPadding,
                paged = paged,
                isLoadingMore = isLoadingMore,
                onLoadMore = onLoadMore,
                modifier = Modifier.weight(1f),
            )
            DetailListLayout.GRID -> GridList(
                items = items,
                onSelect = onSelect,
                bottomPadding = bottomPadding,
                paged = paged,
                isLoadingMore = isLoadingMore,
                onLoadMore = onLoadMore,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowsList(
    items: List<MediaRailItem>,
    onSelect: (Int) -> Unit,
    bottomPadding: Dp,
    paged: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    if (paged) {
        LaunchedEffect(listState, items.size) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey }
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { currentOnLoadMore?.invoke() }
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = items,
            key = { index, item -> "${item.key.ifBlank { item.id.toString() }}#$index" },
        ) { _, item ->
            MediaCoverCard(
                cover = item.image,
                title = item.title,
                // Catalog line as the subtitle: same slot/size as the lists
                // and calendar (`bodyMedium` via MediaCoverCard).
                subtitle = item.meta ?: item.subtitle,
                onClick = { onSelect(item.id) },
                content = {
                    // Extra row (the relation kind): same type and color as
                    // the calendar's episode line. Only when a meta subtitle
                    // took the line above, so it never replaces the title info.
                    if (item.meta != null) {
                        item.subtitle?.let { extra ->
                            Text(
                                text = extra,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
            )
        }
        if (paged) {
            item(key = LoadMoreKey) { LoadMoreFooter(isLoading = isLoadingMore) }
        }
    }
}

@Composable
private fun GridList(
    items: List<MediaRailItem>,
    onSelect: (Int) -> Unit,
    bottomPadding: Dp,
    paged: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    if (paged) {
        LaunchedEffect(gridState, items.size) {
            snapshotFlow {
                gridState.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey }
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { currentOnLoadMore?.invoke() }
        }
    }
    GridCoverSizeProvider { coverSize ->
        LazyVerticalGrid(
            state = gridState,
            // Cells are exactly a standard poster (the token the provider picked
            // from the available width); the leftover becomes even air instead of
            // a wide gutter inside every cell.
            columns = GridCells.FixedSize(coverSize.width),
            modifier = modifier.fillMaxSize(),
            // No horizontal padding: SpaceEvenly owns all the side air, so the
            // gutter between columns equals the one at the edges.
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + bottomPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.key.ifBlank { item.id.toString() }}#$index" },
            ) { _, item ->
                PosterCard(
                    cover = item.image,
                    title = item.title,
                    size = coverSize,
                    onClick = { onSelect(item.id) },
                    subtitle = item.subtitle,
                    placeholderHex = item.placeholderHex,
                    stats = item.stats,
                    // A grid cell lets the subtitle hug the title (no rail to
                    // keep even).
                    reserveTitleLines = false,
                )
            }
            if (paged) {
                item(key = LoadMoreKey, span = { GridItemSpan(maxLineSpan) }) {
                    LoadMoreFooter(isLoading = isLoadingMore)
                }
            }
        }
    }
}
