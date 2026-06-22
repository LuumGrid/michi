package com.luum.michi.app.staffDetail.presentation

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.core.platform.components.PlatformRatingBadge
import com.luum.michi.app.staffDetail.presentation.model.StaffCharacterItem
import com.luum.michi.app.staffDetail.presentation.model.StaffDetail
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaItem
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaSort
import com.luum.michi.app.staffDetail.presentation.state.StaffDetailStateHolder

// Local enum — does NOT conflict with the StaffTab composable in mediaDetail
private enum class StaffDetailTab { OVERVIEW, MEDIA, CHARACTERS }

private val AllSortOptions = StaffMediaSort.entries
private val AllTabs = StaffDetailTab.entries

@Composable
internal fun StaffDetailScreen(
    id: Int,
    stateHolder: StaffDetailStateHolder,
    onOpenMedia: (Int) -> Unit = {},
    onOpenCharacter: (Int) -> Unit = {},
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(id) { stateHolder.load(id) }

    val detail = stateHolder.detail
    when {
        detail != null && detail.id == id -> StaffDetailContent(
            detail = detail,
            stateHolder = stateHolder,
            strings = strings,
            onOpenMedia = onOpenMedia,
            onOpenCharacter = onOpenCharacter,
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
private fun StaffDetailContent(
    detail: StaffDetail,
    stateHolder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = AllTabs[selectedTabIndex.coerceIn(0, AllTabs.lastIndex)]

    Column(modifier = Modifier.fillMaxSize()) {
        StaffDetailHeader(
            detail = detail,
            isFavourite = stateHolder.isFavourite,
            isTogglingFavourite = stateHolder.isTogglingFavourite,
            onToggleFavourite = stateHolder::toggleFavourite,
            strings = strings,
        )

        PlatformChips(
            items = AllTabs,
            selectedItem = selectedTab,
            onSelect = { selectedTabIndex = AllTabs.indexOf(it) },
            label = { tab -> tab.label(strings) },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                StaffDetailTab.OVERVIEW -> StaffOverviewTab(
                    detail = detail,
                    strings = strings,
                )
                StaffDetailTab.MEDIA -> StaffMediaTab(
                    items = stateHolder.mediaItems,
                    isLoadingMore = stateHolder.isLoadingMoreMedia,
                    hasNextPage = stateHolder.mediaHasNextPage,
                    sort = stateHolder.sort,
                    onChangeSort = stateHolder::changeSort,
                    onLoadMore = stateHolder::loadMoreMedia,
                    onOpenMedia = onOpenMedia,
                    strings = strings,
                )
                StaffDetailTab.CHARACTERS -> StaffCharactersTab(
                    items = stateHolder.characterItems,
                    isLoadingMore = stateHolder.isLoadingMoreCharacters,
                    hasNextPage = stateHolder.charactersHasNextPage,
                    onLoadMore = stateHolder::loadMoreCharacters,
                    onOpenCharacter = onOpenCharacter,
                    strings = strings,
                )
            }
        }
    }
}

@Composable
private fun StaffDetailHeader(
    detail: StaffDetail,
    isFavourite: Boolean,
    isTogglingFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    strings: LanguageStrings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (!detail.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = detail.imageUrl,
                    contentDescription = detail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.nativeName.isNullOrBlank()) {
                Text(
                    text = detail.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val favs = detail.favourites
            if (favs != null && favs > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp),
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

        IconButton(onClick = { if (!isTogglingFavourite) onToggleFavourite() }) {
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
private fun StaffOverviewTab(
    detail: StaffDetail,
    strings: LanguageStrings,
) {
    val scrollState = rememberScrollState()
    var bioExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Info grid
        val yearsActiveValue = buildYearsActiveLabel(detail, strings)
        val infoRows = buildList {
            detail.gender?.let { add(strings.infoGenderLabel to it) }
            detail.age?.let { add(strings.infoAgeLabel to it) }
            detail.birthday?.let { add(strings.infoBirthdayLabel to it) }
            detail.death?.let { add(strings.infoDeathLabel to it) }
            yearsActiveValue?.let { add(strings.infoYearsActiveLabel to it) }
            detail.homeTown?.let { add(strings.infoHometownLabel to it) }
            detail.occupations?.let { add(strings.infoOccupationsLabel to it) }
            detail.bloodType?.let { add(strings.infoBloodTypeLabel to it) }
        }
        if (infoRows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                infoRows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(110.dp),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Alternative names
        if (detail.alternativeNames.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.alternativeNamesLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                detail.alternativeNames.forEach { altName ->
                    Text(
                        text = altName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Bio
        if (detail.descriptionPlain.isNotBlank()) {
            val isLongBio = detail.descriptionPlain.length > 280
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.descriptionPlain,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (bioExpanded || !isLongBio) Int.MAX_VALUE else 6,
                    overflow = if (bioExpanded || !isLongBio) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                if (isLongBio) {
                    TextButton(
                        onClick = { bioExpanded = !bioExpanded },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = if (bioExpanded) strings.mediaDetailReadLessAction
                            else strings.mediaDetailReadMoreAction,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun StaffMediaTab(
    items: List<StaffMediaItem>,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    sort: StaffMediaSort,
    onChangeSort: (StaffMediaSort) -> Unit,
    onLoadMore: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    strings: LanguageStrings,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlatformChips(
            items = AllSortOptions,
            selectedItem = sort,
            onSelect = onChangeSort,
            label = { s -> s.toLabel(strings) },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (items.isEmpty() && !isLoadingMore) {
            PlatformListMessage(
                title = strings.staffNoMediaLabel,
                tone = PlatformListMessageTone.Neutral,
            )
            return@Column
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
                StaffMediaCard(item = item, onClick = { onOpenMedia(item.mediaId) })
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
}

@Composable
private fun StaffMediaCard(
    item: StaffMediaItem,
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
        val meta = listOfNotNull(item.staffRole, item.format, item.year?.toString())
            .joinToString(" · ")
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

@Composable
private fun StaffCharactersTab(
    items: List<StaffCharacterItem>,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    onLoadMore: () -> Unit,
    onOpenCharacter: (Int) -> Unit,
    strings: LanguageStrings,
) {
    if (items.isEmpty() && !isLoadingMore) {
        PlatformListMessage(
            title = strings.staffNoCharactersLabel,
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
        items(items = items, key = { it.characterId }) { item ->
            StaffCharacterCard(item = item, onClick = { onOpenCharacter(item.characterId) })
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
private fun StaffCharacterCard(
    item: StaffCharacterItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!item.mediaTitle.isNullOrBlank()) {
            Text(
                text = item.mediaTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ────────────── helpers ──────────────

private fun buildYearsActiveLabel(detail: StaffDetail, strings: LanguageStrings): String? {
    val start = detail.yearsActiveStart ?: return null
    val end = detail.yearsActiveEnd
    return if (end == null || end == 0) {
        "$start – ${strings.yearsActivePresent}"
    } else {
        "$start – $end"
    }
}

private fun StaffDetailTab.label(strings: LanguageStrings): String = when (this) {
    StaffDetailTab.OVERVIEW -> strings.staffTabOverview
    StaffDetailTab.MEDIA -> strings.staffTabMedia
    StaffDetailTab.CHARACTERS -> strings.staffTabCharacters
}

private fun StaffMediaSort.toLabel(strings: LanguageStrings): String = when (this) {
    StaffMediaSort.NEWEST -> strings.sortByNewest
    StaffMediaSort.OLDEST -> strings.sortByOldest
    StaffMediaSort.POPULARITY -> strings.sortByPopularity
    StaffMediaSort.FAVOURITES -> strings.sortByFavourites
    StaffMediaSort.SCORE -> strings.sortByScore
    StaffMediaSort.TITLE -> strings.sortTitle
}
