package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.TestTags

/** One item of a [MediaRail]: cover + two lines, plus an optional accent. */
internal data class MediaRailItem(
    val id: Int,
    val title: String,
    val image: CoverSizes?,
    val subtitle: String? = null,
    // Extra catalog line shown only in the "View all" rows (format · season
    // year · status); the rail stays compact and ignores it.
    val meta: String? = null,
    // Stable identity for LazyRow. Defaults to the id, but callers whose id
    // can repeat (staff with multiple roles) pass a unique edge key.
    val key: String = "",
    /** Cover color painted while the image loads (dashboard rails). */
    val placeholderHex: String? = null,
    // Up to two badges painted over the cover (first = top-start). Empty keeps
    // every existing rail byte-identical.
    val stats: List<CoverStat> = emptyList(),
)

/**
 * Horizontal rail of media/character/staff covers with a section header.
 * Display-only: it never fetches its own pages. The header carries an optional
 * "View all" action ([onViewAll] + [viewAllLabel]) that opens the full list;
 * the rail itself shows only the first items the caller passes. A null
 * [onSelect] renders display-only cards; [emptyLabel] replaces an empty rail
 * with a line of text so a header-only section never looks broken.
 */
@Composable
internal fun MediaRail(
    title: String?,
    items: List<MediaRailItem>,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    viewAllLabel: String? = null,
    emptyLabel: String? = null,
    // Standard poster size for the rail cards: a token, never a raw width.
    // Only the dashboard's featured rail passes a different one.
    size: CoverSize = CoverSize.Rail,
    // When the caller's item set changes because of a filter/order, it passes
    // the filter value here and the rail returns to the start (scroll rule).
    resetKey: Any? = null,
    // Painted over each card's cover (e.g. an edit-mode remove button). When
    // set, the card no longer navigates: the overlay owns the interaction.
    cardOverlay: (@Composable BoxScope.(MediaRailItem) -> Unit)? = null,
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
            LazyRow(
                state = rowState,
                modifier = Modifier.testTag(TestTags.Rail),
                contentPadding = PaddingValues(horizontal = RailHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = items,
                    // Index-suffixed: the same media can appear more than once
                    // (a staff/character with multiple roles in one title), and
                    // a bare id would make duplicate lazy keys and crash.
                    key = { index, item -> "${item.key.ifBlank { item.id.toString() }}#$index" },
                ) { _, item ->
                    PosterCard(
                        cover = item.image,
                        title = item.title,
                        size = size,
                        onClick = if (cardOverlay == null) {
                            onSelect?.let { select -> { select(item.id) } }
                        } else {
                            null
                        },
                        subtitle = item.subtitle,
                        placeholderHex = item.placeholderHex,
                        stats = item.stats,
                        overlay = cardOverlay?.let { overlay -> { overlay(this, item) } },
                    )
                }
            }
        }
    }
}
