package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.ui.icons.AppIcons

/** One item of a [MediaRail]: cover + two lines, plus an optional accent. */
internal data class MediaRailItem(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val subtitle: String? = null,
    // Extra catalog line shown only in the "View all" rows (format · season
    // year · status); the rail stays compact and ignores it.
    val meta: String? = null,
    // Stable identity for LazyRow. Defaults to the id, but callers whose id
    // can repeat (staff with multiple roles) pass a unique edge key.
    val key: String = "",
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
    // Baked 16dp matches the full-width layout; panels (already inset) pass
    // zero to avoid double padding.
    horizontalPadding: Dp = 16.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(
                    title = title,
                    modifier = Modifier.weight(1f),
                )
                if (onViewAll != null && viewAllLabel != null) {
                    Row(
                        modifier = Modifier.clickable(onClick = onViewAll),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = viewAllLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (items.isEmpty() && emptyLabel != null) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = items,
                    // Index-suffixed: the same media can appear more than once
                    // (a staff/character with multiple roles in one title), and
                    // a bare id would make duplicate lazy keys and crash.
                    key = { index, item -> "${item.key.ifBlank { item.id.toString() }}#$index" },
                ) { _, item ->
                    RailCard(item = item, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun RailCard(
    item: MediaRailItem,
    onSelect: ((Int) -> Unit)?,
) {
    val clickable = if (onSelect != null) {
        Modifier.clickable { onSelect(item.id) }
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .width(108.dp)
            .then(clickable),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
