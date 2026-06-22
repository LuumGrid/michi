package com.luum.michi.app.studioDetail.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.core.platform.components.PlatformRatingBadge
import com.luum.michi.app.studioDetail.presentation.model.StudioDetail
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaItem
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaSort
import com.luum.michi.app.studioDetail.presentation.state.StudioDetailStateHolder

private val AllSortOptions = StudioMediaSort.entries

@Composable
internal fun StudioDetailScreen(
    id: Int,
    stateHolder: StudioDetailStateHolder,
    onOpenMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(id) { stateHolder.load(id) }

    val detail = stateHolder.detail
    when {
        detail != null && detail.id == id -> StudioDetailContent(
            detail = detail,
            stateHolder = stateHolder,
            strings = strings,
            onOpenMedia = onOpenMedia,
        )
        stateHolder.isLoading -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
        stateHolder.error != null -> PlatformListMessage(
            title = strings.mediaDetailErrorLabel,
            subtitle = stateHolder.error?.let { strings.networkErrorMessage(it) },
            tone = PlatformListMessageTone.Error,
        )
        else -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
    }
}

@Composable
private fun StudioDetailContent(
    detail: StudioDetail,
    stateHolder: StudioDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StudioDetailHeader(
            detail = detail,
            isFavourite = stateHolder.isFavourite,
            isTogglingFavourite = stateHolder.isTogglingFavourite,
            onToggleFavourite = stateHolder::toggleFavourite,
            strings = strings,
        )

        PlatformChips(
            items = AllSortOptions,
            selectedItem = stateHolder.sort,
            onSelect = stateHolder::changeSort,
            label = { sort -> sort.toLabel(strings) },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        StudioMediaGrid(
            items = stateHolder.mediaItems,
            isLoadingMore = stateHolder.isLoadingMore,
            hasNextPage = stateHolder.hasNextPage,
            onLoadMore = stateHolder::loadMore,
            onOpenMedia = onOpenMedia,
            emptyLabel = strings.studioNoMediaLabel,
        )
    }
}

@Composable
private fun StudioDetailHeader(
    detail: StudioDetail,
    isFavourite: Boolean,
    isTogglingFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    strings: LanguageStrings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isAnimationStudio) {
                Text(
                    text = strings.studioAnimationLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val favs = detail.favourites
            if (favs != null && favs > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        painter = PlatformIcons.Like,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = favs.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        IconButton(
            onClick = { if (!isTogglingFavourite) onToggleFavourite() },
        ) {
            Icon(
                painter = if (isFavourite) PlatformIcons.LikeFilled else PlatformIcons.Like,
                contentDescription = strings.favouriteLabel,
                tint = if (isFavourite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudioMediaGrid(
    items: List<StudioMediaItem>,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    onLoadMore: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    emptyLabel: String,
) {
    if (items.isEmpty() && !isLoadingMore) {
        PlatformListMessage(
            title = emptyLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, hasNextPage) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { last ->
            if (last >= items.size - 6 && hasNextPage && !isLoadingMore) {
                onLoadMore()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it.mediaId }) { item ->
            StudioMediaCard(item = item, onClick = { onOpenMedia(item.mediaId) })
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StudioMediaCard(
    item: StudioMediaItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        PlatformMediaCover(
            coverUrl = item.coverUrl,
            palette = item.palette,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PlatformCoverSize.PosterAspectRatio),
            overlay = {
                val score = item.averageScore
                if (score != null && score > 0) {
                    PlatformRatingBadge(averageScore = score)
                }
            },
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        val meta = listOfNotNull(item.format, item.year?.toString()).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun StudioMediaSort.toLabel(strings: LanguageStrings): String = when (this) {
    StudioMediaSort.POPULARITY -> strings.sortByPopularity
    StudioMediaSort.NEWEST -> strings.sortByNewest
    StudioMediaSort.OLDEST -> strings.sortByOldest
    StudioMediaSort.FAVOURITES -> strings.sortByFavourites
    StudioMediaSort.SCORE -> strings.sortByScore
}
