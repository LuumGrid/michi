package com.luum.michi.app.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.TestTags

/** How many cards a column of [MediaCardRail] stacks. */
internal enum class MediaCardRailRows(val count: Int) {
    ONE(1),
    THREE(3),
}

/**
 * Visible sliver of the next column: the column width is the rail's visible
 * width minus this peek (and the gap), so the rail always signals that more
 * content waits to the side. A token, never a raw width at the call site.
 */
internal val MediaCardRailPeek = 40.dp

private val MediaCardRailGap = 12.dp
private val MediaCardRailRowGap = 8.dp

/**
 * Horizontal rail of media cards with the standard rail header (title +
 * "View all"). Every card is a [MediaCoverCard], so Discover, the detail and the
 * profile share the list language instead of poster covers.
 *
 * Two layouts: [MediaCardRailRows.ONE] leads with one large card ([CoverSize.List])
 * and [MediaCardRailRows.THREE] draws columns of three compact cards
 * ([CoverSize.Compact]) stacked like a store shelf. Columns snap while scrolling,
 * the last column keeps the same height even with fewer than three cards, and
 * [numbered] adds 1, 2, 3… as a chip at the start of the card's chip row, beside
 * the rating (never over the cover).
 *
 * Display-only: it never fetches. A null [onSelect] paints cards whose tap does
 * nothing; the caller passes the first items it already has. [resetKey]
 * re-creates the scroll state in place when the item set changes (the app-wide
 * scroll rule).
 */
@Composable
internal fun MediaCardRail(
    title: String?,
    items: List<MediaRailItem>,
    modifier: Modifier = Modifier,
    rows: MediaCardRailRows = MediaCardRailRows.THREE,
    numbered: Boolean = false,
    onSelect: ((Int) -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    viewAllLabel: String? = null,
    emptyLabel: String? = null,
    resetKey: Any? = null,
) {
    val rowState = rememberResetListState(resetKey)
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            RailHeader(
                title = title,
                viewAllLabel = viewAllLabel,
                onViewAll = onViewAll,
            )
        }
        if (items.isEmpty() && emptyLabel != null) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RailHorizontalPadding, vertical = 8.dp),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // One column per viewport minus the peek: the next card always
                // shows a sliver, and snapping lands on a column boundary.
                val columnWidth = (
                    maxWidth - RailHorizontalPadding * 2 - MediaCardRailGap - MediaCardRailPeek
                ).coerceAtLeast(0.dp)
                val compact = rows == MediaCardRailRows.THREE
                val coverSize = if (compact) CoverSize.Compact else CoverSize.List
                // Fixed column height, even for the last column with one or two
                // cards: the rail never changes height while scrolling.
                val cardHeight = mediaCoverCardHeight(coverSize)
                val columnHeight = (cardHeight + MediaCardRailRowGap) * rows.count -
                    MediaCardRailRowGap
                val columns = items.chunked(rows.count)
                LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Rail),
                    contentPadding = PaddingValues(horizontal = RailHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(MediaCardRailGap),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
                ) {
                    itemsIndexed(
                        items = columns,
                        key = { index, _ -> "column#$index" },
                    ) { columnIndex, column ->
                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .height(columnHeight),
                            verticalArrangement = Arrangement.spacedBy(MediaCardRailRowGap),
                        ) {
                            column.forEachIndexed { rowIndex, item ->
                                MediaCardRailCard(
                                    item = item,
                                    rank = columnIndex * rows.count + rowIndex + 1,
                                    compact = compact,
                                    numbered = numbered,
                                    onClick = { onSelect?.invoke(item.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaCardRailCard(
    item: MediaRailItem,
    rank: Int,
    compact: Boolean,
    numbered: Boolean,
    onClick: () -> Unit,
) {
    // Compact cards carry the lead stat only (the one the rail is sorted by);
    // the one-row card has room for the two chips. FlowRow wraps instead of
    // clipping when the column gets narrow.
    val visibleStats = if (compact) item.stats.take(1) else item.stats.take(2)
    MediaCoverCard(
        cover = item.image,
        title = item.title,
        subtitle = item.subtitle,
        onClick = onClick,
        modifier = Modifier.testTag(TestTags.Poster),
        titleMaxLines = if (compact) 1 else 2,
        subtitleMaxLines = 1,
        coverSize = if (compact) CoverSize.Compact else CoverSize.List,
        content = {
            if (numbered || visibleStats.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Ranking chips lead the stats, beside the rating.
                    if (numbered) {
                        MediaCardRailRankChip(rank)
                    }
                    visibleStats.forEach { stat ->
                        StatPill(value = stat.value, icon = stat.icon)
                    }
                }
            }
        },
    )
}

/**
 * The rank chip label lives here: the `#` keeps it apart from a smiley score
 * chip (which is also a text-only pill), and every rail formats it the same way.
 */
@Composable
private fun MediaCardRailRankChip(rank: Int) {
    StatPill(value = "#$rank", icon = null)
}
