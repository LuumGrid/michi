package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.RefreshErrorRow

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
    // Current page of the backing PagedList. The auto-paging effect keys on it
    // (not on items.size): a page that dedupes to nothing must still re-trigger
    // the load, while a failed page never advances it (so it cannot loop).
    loadedPage: Int,
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
                loadedPage = loadedPage,
                onLoadMore = onLoadMore,
                modifier = Modifier.weight(1f),
            )
            DetailListLayout.GRID -> DetailPosterGrid(
                items = items,
                onSelect = onSelect,
                // The grid's own empty row is off: this list has no empty copy.
                isLoading = false,
                isLoadingMore = isLoadingMore,
                hasNextPage = paged,
                loadedPage = loadedPage,
                onLoadMore = { onLoadMore?.invoke() },
                emptyLabel = null,
                modifier = Modifier.weight(1f),
                bottomPadding = bottomPadding,
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
    loadedPage: Int,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(
        state = listState,
        enabled = paged,
        loadedPage = loadedPage,
        onLoadMore = { onLoadMore?.invoke() },
    )
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
