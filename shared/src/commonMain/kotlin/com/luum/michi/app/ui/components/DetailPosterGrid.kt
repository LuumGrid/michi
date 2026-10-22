package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One full-width section of a poster grid; [key] identifies its header. */
internal data class PosterGridSection(
    val key: String,
    val title: String,
    val items: List<MediaRailItem>,
)

/**
 * The one poster grid of a detail page (studio works, genre works, the "View
 * all" people grids): token-sized cells over [MediaRailItem] that page
 * themselves when the footer scrolls into view. [header] paints the page's own
 * full-span items above the grid (a hero, chip rows) so each screen keeps its
 * own header without forking the grid; [resetKey] is the holder state that must
 * send the grid back to the top (a sort change, a catalog switch). [emptyLabel]
 * null paints no empty row (the caller has its own empty handling).
 *
 * [sections] is optional: null keeps the flat grid unchanged (genre, catalog,
 * favourites). When present, each section paints a full-span [SectionHeader]
 * above its cards, using the same horizontal inset as the rails.
 */
@Composable
internal fun DetailPosterGrid(
    items: List<MediaRailItem>,
    onSelect: (Int) -> Unit,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    // Current page of the backing PagedList: the auto-paging effect keys on it
    // (not on items.size) so a fully-deduped page still advances the load, while
    // a failed page never does (no retry loop).
    loadedPage: Int,
    onLoadMore: () -> Unit,
    emptyLabel: String?,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    bottomPadding: Dp = 0.dp,
    header: (LazyGridScope.() -> Unit)? = null,
    sections: List<PosterGridSection>? = null,
    // Painted over each card's cover (e.g. an edit-mode remove button). When set,
    // the card no longer navigates: the overlay owns the interaction.
    cardOverlay: (@Composable BoxScope.(MediaRailItem) -> Unit)? = null,
) {
    val gridState = rememberResetGridState(resetKey)
    LoadMoreEffect(
        state = gridState,
        enabled = hasNextPage,
        loadedPage = loadedPage,
        onLoadMore = onLoadMore,
    )
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
            if (items.isEmpty() && !isLoading && emptyLabel != null) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = emptyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                if (sections == null) {
                    itemsIndexed(
                        items = items,
                        // Index-suffixed: the same media can appear more than once
                        // (a staff/character with multiple roles in one title), and
                        // a bare id would make duplicate lazy keys and crash.
                        key = { index, item -> "${item.key.ifBlank { item.id.toString() }}#$index" },
                    ) { _, item ->
                        PosterGridCard(item, coverSize, onSelect, cardOverlay)
                    }
                } else {
                    var globalIndex = 0
                    sections.forEachIndexed { sectionIndex, section ->
                        item(
                            key = "section-$sectionIndex-${section.key}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(
                                    horizontal = RailHorizontalPadding,
                                    vertical = 4.dp,
                                ),
                            )
                        }
                        val startIndex = globalIndex
                        itemsIndexed(
                            items = section.items,
                            key = { index, item ->
                                "${item.key.ifBlank { item.id.toString() }}#${startIndex + index}"
                            },
                        ) { _, item ->
                            PosterGridCard(item, coverSize, onSelect, cardOverlay)
                        }
                        globalIndex += section.items.size
                    }
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

@Composable
private fun PosterGridCard(
    item: MediaRailItem,
    coverSize: CoverSize,
    onSelect: (Int) -> Unit,
    cardOverlay: (@Composable BoxScope.(MediaRailItem) -> Unit)?,
) {
    PosterCard(
        cover = item.image,
        title = item.title,
        size = coverSize,
        onClick = if (cardOverlay == null) {
            { onSelect(item.id) }
        } else {
            null
        },
        subtitle = item.subtitle,
        placeholderHex = item.placeholderHex,
        stats = item.stats,
        overlay = cardOverlay?.let { overlay -> { overlay(this, item) } },
    )
}
