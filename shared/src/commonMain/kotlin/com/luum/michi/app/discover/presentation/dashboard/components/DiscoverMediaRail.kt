package com.luum.michi.app.discover.presentation.dashboard.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.DiscoverPoster
import com.luum.michi.app.ui.components.DiscoverSection
import com.luum.michi.app.ui.components.FavouritesBadge
import com.luum.michi.app.ui.components.RatingBadge

@Composable
fun DiscoverMediaRail(
    title: String,
    items: List<MediaItem>,
    onItemClick: ((Int) -> Unit)? = null,
    onItemLongClick: ((Int) -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    DiscoverSection(title = title, onSeeAll = onSeeAll) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { index, item -> "$index-${item.id}" }) { _, item ->
                DiscoverMediaCard(
                    item = item,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverMediaCard(
    item: MediaItem,
    onClick: ((Int) -> Unit)? = null,
    onLongClick: ((Int) -> Unit)? = null,
) {
    val clickModifier = if (item.id != null && (onClick != null || onLongClick != null)) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke(item.id) },
            onLongClick = onLongClick?.let { handler -> { handler(item.id) } },
        )
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .width(CoverSize.RailPosterWidth)
            .then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DiscoverPoster(
            paletteHex = item.paletteHex,
            coverUrl = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CoverSize.PosterAspectRatio),
        ) {
            if (item.averageScore != null) {
                RatingBadge(averageScore = item.averageScore, isUserRanked = item.isUserRanked)
            }
            if (item.favourites != null && item.favourites > 0) {
                FavouritesBadge(favourites = item.favourites, isUserFavorited = item.isUserFavorited)
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
