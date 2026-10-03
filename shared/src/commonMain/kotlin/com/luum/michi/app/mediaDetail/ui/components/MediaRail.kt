package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/** One item of a [MediaRail]: cover + two lines, plus an optional accent. */
internal data class MediaRailItem(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val subtitle: String? = null,
)

/**
 * Horizontal rail of media/character/staff covers with a section header.
 * Shared by the detail page sections (relations, recommendations, characters,
 * staff). A null [onSelect] renders display-only cards (character/staff taps
 * land with their own screens); [onLoadMore] adds a footer when more pages
 * exist. Pure primitives + a lambda, so any detail scope reuses it.
 */
@Composable
internal fun MediaRail(
    title: String,
    items: List<MediaRailItem>,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    loadMoreLabel: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items, key = { it.id }) { item ->
                RailCard(item = item, onSelect = onSelect)
            }
        }
        if (onLoadMore != null && loadMoreLabel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = loadMoreLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLoadMore)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
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
