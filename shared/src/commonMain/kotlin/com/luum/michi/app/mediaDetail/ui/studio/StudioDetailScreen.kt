package com.luum.michi.app.mediaDetail.ui.studio

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaItem
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort
import com.luum.michi.app.mediaDetail.domain.studio.model.label
import com.luum.michi.app.mediaDetail.domain.studio.model.sortedFor
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailSectionHeader
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Studio detail page: name hero + a 2-column media grid with its own local
 * score/year sort and client-side type/format filters. The `+` button (signed
 * in only) opens the entry editor. Reached from a media information panel
 * studio chip via [com.luum.michi.app.root.state.State.openStudio].
 */
@Composable
internal fun StudioDetailScreen(
    holder: StudioDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onAddEntry: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    when {
        detail == null && holder.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.mediaDetailLoadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        detail == null && holder.error != null -> {
            MessagePanel(
                title = strings.mediaDetailErrorLabel,
                message = holder.error?.let { strings.networkErrorMessage(it) },
                icon = Icons.Filled.Warning,
                actionLabel = strings.retryAction,
                onAction = onRetry,
                modifier = modifier,
            )
        }
        detail != null -> {
            PullRefresh(isRefreshing = holder.isLoading, onRefresh = onRetry) {
                StudioDetailGrid(
                    detail = detail,
                    holder = holder,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onOpenMedia = onOpenMedia,
                    onAddEntry = onAddEntry,
                    modifier = modifier,
                )
            }
        }
    }
}

private val StudioSortOptions = listOf(
    StudioMediaSort.SCORE,
    StudioMediaSort.NEWEST,
    StudioMediaSort.OLDEST,
)

@Composable
private fun StudioDetailGrid(
    detail: StudioDetail,
    holder: StudioDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenMedia: (Int) -> Unit,
    onAddEntry: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Filter/sort are screen-local: switching them never touches the holder
    // or the network (the list is already loaded, ordered by popularity).
    var typeFilter by remember { mutableStateOf<String?>(null) }
    var formatFilter by remember { mutableStateOf<String?>(null) }

    val formats = remember(holder.mediaItems) {
        holder.mediaItems.mapNotNull { it.format }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filtered = holder.mediaItems
        .sortedFor(holder.sort)
        .filter { item ->
            (typeFilter == null || item.type == typeFilter) &&
                (formatFilter == null || item.format == formatFilter)
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp + bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
            StudioHero(detail = detail, strings = strings)
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            StudioFilterControls(
                strings = strings,
                selectedSort = holder.sort,
                onSelectSort = holder::changeSort,
                typeFilter = typeFilter,
                onSelectType = { typeFilter = it },
                formats = formats,
                formatFilter = formatFilter,
                onSelectFormat = { formatFilter = it },
            )
        }
        if (filtered.isEmpty() && !holder.isLoading) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = strings.mediaDetailNoMediaLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        } else {
            items(items = filtered, key = { it.mediaId }) { item ->
                StudioMediaGridCard(
                    item = item,
                    strings = strings,
                    onOpenMedia = onOpenMedia,
                    onAddEntry = onAddEntry,
                )
            }
            if (holder.hasNextPage) {
                item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = strings.mediaDetailLoadMoreAction,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !holder.isLoadingMore) { holder.loadMore() }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioFilterControls(
    strings: LanguageStrings,
    selectedSort: StudioMediaSort,
    onSelectSort: (StudioMediaSort) -> Unit,
    typeFilter: String?,
    onSelectType: (String?) -> Unit,
    formats: List<String>,
    formatFilter: String?,
    onSelectFormat: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailMediaTitle,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChipRail(
            tabs = StudioSortOptions.map { ChipTab(value = it, label = it.label(strings)) },
            selected = selectedSort,
            onSelect = onSelectSort,
            contentPadding = PaddingValues(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val typeTabs: List<ChipTab<String?>> = listOf(
            ChipTab(value = null, label = strings.sectionAll),
            ChipTab(value = "ANIME", label = strings.animeLabel),
            ChipTab(value = "MANGA", label = strings.mangaLabel),
        )
        ChipRail(
            tabs = typeTabs,
            selected = typeFilter,
            onSelect = onSelectType,
            contentPadding = PaddingValues(horizontal = 8.dp),
        )
        if (formats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val formatTabs: List<ChipTab<String?>> = buildList {
                add(ChipTab(value = null, label = strings.sectionAll))
                formats.forEach { add(ChipTab(value = it, label = it)) }
            }
            ChipRail(
                tabs = formatTabs,
                selected = formatFilter,
                onSelect = onSelectFormat,
                contentPadding = PaddingValues(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun StudioHero(detail: StudioDetail, strings: LanguageStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = detail.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (detail.isAnimeStudio) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.mediaDetailAnimeStudioLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        detail.favourites?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$it ${strings.favoritesLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StudioMediaGridCard(
    item: StudioMediaItem,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
    onAddEntry: ((Int) -> Unit)?,
) {
    Surface(
        onClick = { onOpenMedia(item.mediaId) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (!item.coverUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        loading = {},
                        error = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (item.averageScore != null) {
                    CoverBadge(
                        icon = AppIcons.StarOutline,
                        text = item.averageScore.toString(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }
                if (item.favourites != null) {
                    CoverBadge(
                        icon = AppIcons.Favorite,
                        text = item.favourites.toString(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
                if (onAddEntry != null) {
                    Surface(
                        onClick = { onAddEntry(item.mediaId) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = strings.editEntryAction,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.genres.take(2).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Translucent pill over a cover: icon + compact count. */
@Composable
private fun CoverBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.55f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
