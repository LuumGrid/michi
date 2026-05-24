package com.luum.michi.app.mediaDetail.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformFavouritesBadge
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformRatingBadge
import com.luum.michi.app.mediaDetail.presentation.model.*
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

private enum class DetailTab {
    OVERVIEW,
    CONNECTIONS,
    CHARACTERS,
    STAFF,
    REVIEWS,
    THREADS,
    FOLLOWING,
    ACTIVITIES,
    STATS,
}

private val VoiceLanguageOptions = listOf(
    "JAPANESE",
    "ENGLISH",
    "KOREAN",
    "SPANISH",
    "FRENCH",
    "GERMAN",
    "ITALIAN",
    "PORTUGUESE",
    "HEBREW",
    "HUNGARIAN",
)

@Composable
internal fun MediaDetailScreen(
    mediaId: Int,
    stateHolder: MediaDetailStateHolder,
    onRequestEdit: (Int) -> Unit,
    onOpenRelation: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(mediaId) { stateHolder.load(mediaId) }

    val detail = stateHolder.detail
    when {
        detail != null && detail.id == mediaId -> MediaDetailContent(
            detail = detail,
            stateHolder = stateHolder,
            strings = strings,
            onEditClick = { onRequestEdit(mediaId) },
            onOpenRelation = onOpenRelation,
        )
        stateHolder.isLoading -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
        stateHolder.error != null -> PlatformListMessage(
            title = strings.mediaDetailErrorLabel,
            subtitle = stateHolder.error,
            tone = PlatformListMessageTone.Error,
        )
        else -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
    }
}

@Composable
private fun MediaDetailContent(
    detail: MediaDetail,
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
    onEditClick: () -> Unit,
    onOpenRelation: (Int) -> Unit,
) {
    var selectedTab by remember(detail.id) { mutableStateOf(DetailTab.OVERVIEW) }

    Column(modifier = Modifier.fillMaxSize()) {
        MediaDetailHeader(detail = detail)
        MediaDetailEntryAction(detail = detail, strings = strings, onClick = onEditClick)
        MediaDetailTabBar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            strings = strings,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                DetailTab.OVERVIEW -> OverviewTab(
                    detail = detail,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
                DetailTab.CONNECTIONS -> ConnectionsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
                DetailTab.CHARACTERS -> CharactersTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.STAFF -> StaffTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.REVIEWS -> ReviewsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.THREADS -> ThreadsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.FOLLOWING -> FollowingTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.ACTIVITIES -> ActivitiesTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.STATS -> StatsTab(
                    detail = detail,
                    strings = strings,
                )
            }
        }
    }
}

private fun DetailTab.label(strings: LanguageStrings): String = when (this) {
    DetailTab.OVERVIEW -> strings.mediaDetailTabOverview
    DetailTab.CONNECTIONS -> strings.mediaDetailTabConnections
    DetailTab.CHARACTERS -> strings.mediaDetailTabCharacters
    DetailTab.STAFF -> strings.mediaDetailTabStaff
    DetailTab.REVIEWS -> strings.mediaDetailTabReviews
    DetailTab.THREADS -> strings.mediaDetailTabThreads
    DetailTab.FOLLOWING -> strings.mediaDetailTabFollowing
    DetailTab.ACTIVITIES -> strings.mediaDetailTabActivities
    DetailTab.STATS -> strings.mediaDetailTabStats
}

@Composable
private fun MediaDetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    strings: LanguageStrings,
) {
    val tabs = remember { DetailTab.entries }
    PlatformChips(
        items = tabs,
        selectedItem = selected,
        onSelect = onSelect,
        label = { tab -> tab.label(strings) },
        useSoftActiveColor = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MediaDetailEntryAction(
    detail: MediaDetail,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    val label = if (detail.viewerEntry != null) strings.mediaDetailEditEntryAction
    else strings.mediaDetailAddToListAction
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaDetailHeader(detail: MediaDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.verticalGradient(detail.palette)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlatformMediaCover(
                coverUrl = detail.coverUrl,
                palette = detail.palette,
                contentDescription = detail.title,
                modifier = Modifier
                    .width(PlatformCoverSize.RailPosterWidth)
                    .aspectRatio(PlatformCoverSize.PosterAspectRatio),
                fallbackIcon = PlatformIcons.Home,
                fallbackIconSize = 34.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    detail.format,
                    detail.season,
                    detail.startedLabel?.takeIf { detail.season == null },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (detail.averageScore != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "${detail.averageScore}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    detail: MediaDetail,
    strings: LanguageStrings,
    onOpenRelation: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { MediaDetailOverviewSection(detail = detail, strings = strings) }
        if (detail.genres.isNotEmpty()) {
            item { MediaDetailGenres(genres = detail.genres, strings = strings) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item { MediaDetailDescription(text = detail.descriptionPlain, strings = strings) }
        }
        if (detail.studios.isNotEmpty()) {
            item { MediaDetailStudios(studios = detail.studios, strings = strings) }
        }
    }
}

@Composable
private fun StatsTab(detail: MediaDetail, strings: LanguageStrings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (detail.statusDistribution.isNotEmpty()) {
            item {
                MediaDetailSection(title = strings.mediaDetailStatusDistributionTitle) {
                    StatusDistributionList(buckets = detail.statusDistribution, strings = strings)
                }
            }
        }
        if (detail.scoreDistribution.isNotEmpty()) {
            item {
                MediaDetailSection(title = strings.mediaDetailScoreDistributionTitle) {
                    ScoreDistributionList(buckets = detail.scoreDistribution)
                }
            }
        }
    }
}

@Composable
private fun StatusDistributionList(
    buckets: List<MediaStatusBucket>,
    strings: LanguageStrings,
) {
    val total = buckets.sumOf { it.amount }.coerceAtLeast(1)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        buckets.forEach { bucket ->
            val pct = bucket.amount.toFloat() / total
            DistributionBar(
                label = statusLabel(bucket.status, strings),
                value = bucket.amount.toString(),
                fraction = pct,
                color = statusColor(bucket.status),
            )
        }
    }
}

@Composable
private fun ScoreDistributionList(buckets: List<MediaScoreBucket>) {
    val max = buckets.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buckets.forEach { bucket ->
            val pct = bucket.amount.toFloat() / max
            DistributionBar(
                label = "${bucket.score}",
                value = bucket.amount.toString(),
                fraction = pct,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DistributionBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

private fun statusLabel(status: MediaStatsStatus, strings: LanguageStrings): String = when (status) {
    MediaStatsStatus.CURRENT -> strings.mediaStatsStatusCurrent
    MediaStatsStatus.PLANNING -> strings.mediaStatsStatusPlanning
    MediaStatsStatus.COMPLETED -> strings.mediaStatsStatusCompleted
    MediaStatsStatus.DROPPED -> strings.mediaStatsStatusDropped
    MediaStatsStatus.PAUSED -> strings.mediaStatsStatusPaused
    MediaStatsStatus.REPEATING -> strings.mediaStatsStatusRepeating
    MediaStatsStatus.OTHER -> ""
}

@Composable
private fun statusColor(status: MediaStatsStatus): Color = when (status) {
    MediaStatsStatus.CURRENT -> MaterialTheme.colorScheme.primary
    MediaStatsStatus.PLANNING -> MaterialTheme.colorScheme.tertiary
    MediaStatsStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    MediaStatsStatus.DROPPED -> MaterialTheme.colorScheme.error
    MediaStatsStatus.PAUSED -> MaterialTheme.colorScheme.outline
    MediaStatsStatus.REPEATING -> MaterialTheme.colorScheme.primary
    MediaStatsStatus.OTHER -> MaterialTheme.colorScheme.outlineVariant
}

@Composable
private fun CharactersTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VoiceLanguageChipRow(
            selected = stateHolder.voiceLanguage,
            onSelect = stateHolder::selectVoiceLanguage,
            strings = strings,
        )
        val characters = stateHolder.characters
        if (characters.isEmpty() && !stateHolder.isLoadingCharacters) {
            PlatformListMessage(
                title = strings.mediaDetailNoCharactersLabel,
                tone = PlatformListMessageTone.Neutral,
            )
            return@Column
        }
        val gridState = rememberLazyGridState()
        LaunchedEffect(gridState, stateHolder.charactersHasNextPage) {
            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible
            }.collect { last ->
                if (last >= characters.size - 4 && stateHolder.charactersHasNextPage && !stateHolder.isLoadingCharacters) {
                    stateHolder.loadMoreCharacters()
                }
            }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            gridItems(items = characters, key = { it.edgeKey }) { entry ->
                CharacterCard(entry = entry, strings = strings)
            }
            if (stateHolder.isLoadingCharacters) {
                item { LoadingTile() }
                item { LoadingTile() }
            }
        }
    }
}

@Composable
private fun VoiceLanguageChipRow(
    selected: String,
    onSelect: (String) -> Unit,
    strings: LanguageStrings,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = VoiceLanguageOptions, key = { it }) { lang ->
            FilterChip(
                selected = lang == selected,
                onClick = { onSelect(lang) },
                label = { Text(voiceLanguageLabel(lang, strings)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

private fun voiceLanguageLabel(code: String, strings: LanguageStrings): String = when (code) {
    "JAPANESE" -> strings.voiceLanguageJapanese
    "ENGLISH" -> strings.voiceLanguageEnglish
    "KOREAN" -> strings.voiceLanguageKorean
    "ITALIAN" -> strings.voiceLanguageItalian
    "SPANISH" -> strings.voiceLanguageSpanish
    "PORTUGUESE" -> strings.voiceLanguagePortuguese
    "FRENCH" -> strings.voiceLanguageFrench
    "GERMAN" -> strings.voiceLanguageGerman
    "HEBREW" -> strings.voiceLanguageHebrew
    "HUNGARIAN" -> strings.voiceLanguageHungarian
    else -> code
}

@Composable
private fun CharacterCard(entry: MediaCharacterEntry, strings: LanguageStrings) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PersonHalf(
                imageUrl = entry.imageUrl,
                name = entry.name,
                subtitle = roleLabel(entry.role, strings),
                modifier = Modifier.weight(1f),
            )
            entry.voiceActor?.let { va ->
                PersonHalf(
                    imageUrl = va.imageUrl,
                    name = va.name,
                    subtitle = va.language ?: "",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PersonHalf(
    imageUrl: String?,
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun roleLabel(role: MediaCharacterRole, strings: LanguageStrings): String = when (role) {
    MediaCharacterRole.MAIN -> strings.mediaDetailCharacterRoleMain
    MediaCharacterRole.SUPPORTING -> strings.mediaDetailCharacterRoleSupporting
    MediaCharacterRole.BACKGROUND -> strings.mediaDetailCharacterRoleBackground
    MediaCharacterRole.OTHER -> ""
}

@Composable
private fun StaffTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val staff = stateHolder.staff
    if (staff.isEmpty() && !stateHolder.isLoadingStaff) {
        PlatformListMessage(
            title = strings.mediaDetailNoStaffLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= staff.size - 4
        }
    }
    LaunchedEffect(nearEnd, stateHolder.staffHasNextPage) {
        if (nearEnd && stateHolder.staffHasNextPage && !stateHolder.isLoadingStaff) {
            stateHolder.loadMoreStaff()
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(items = staff, key = { it.edgeKey }) { entry ->
            StaffCard(entry = entry)
        }
        if (stateHolder.isLoadingStaff) {
            item { LoadingTile() }
            item { LoadingTile() }
        }
    }
}

@Composable
private fun StaffCard(entry: MediaStaffEntry) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        PersonHalf(
            imageUrl = entry.imageUrl,
            name = entry.name,
            subtitle = entry.role.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingTile() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MediaDetailOverviewSection(detail: MediaDetail, strings: LanguageStrings) {
    val rows = buildList {
        detail.format?.let { add(strings.mediaDetailFormatLabel to it) }
        detail.status?.let { add(strings.mediaDetailStatusLabel to it) }
        when (detail.type) {
            MediaDetailType.MANGA -> {
                detail.chapters?.let { add(strings.mediaDetailChaptersLabel to it.toString()) }
                detail.volumes?.let { add(strings.mediaDetailVolumesLabel to it.toString()) }
            }
            else -> {
                detail.episodes?.let { add(strings.mediaDetailEpisodesLabel to it.toString()) }
                detail.duration?.let { add(strings.mediaDetailDurationLabel to "$it min") }
            }
        }
        detail.season?.let { add(strings.mediaDetailSeasonLabel to it) }
        detail.startedLabel?.let { add(strings.mediaDetailStartedLabel to it) }
        detail.endedLabel?.let { add(strings.mediaDetailEndedLabel to it) }
        detail.source?.let { add(strings.mediaDetailSourceLabel to it) }
        detail.meanScore?.let { add(strings.mediaDetailMeanScoreLabel to "$it%") }
        detail.popularity?.let { add(strings.mediaDetailPopularityLabel to it.toString()) }
        detail.favourites?.let { add(strings.mediaDetailFavoritesLabel to it.toString()) }
    }
    if (rows.isEmpty()) return

    MediaDetailSection(title = strings.mediaDetailOverviewTitle) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        modifier = Modifier.width(140.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaDetailGenres(genres: List<String>, strings: LanguageStrings) {
    MediaDetailSection(title = strings.mediaDetailGenresTitle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunks = genres.chunked(3)
                chunks.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { genre ->
                            AssistChip(
                                onClick = {},
                                label = { Text(genre) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailDescription(text: String, strings: LanguageStrings) {
    var expanded by remember(text) { mutableStateOf(false) }
    val isLong = text.length > 280
    MediaDetailSection(title = strings.mediaDetailDescriptionTitle) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded || !isLong) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLong) {
                Text(
                    text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
        }
    }
}

@Composable
private fun MediaDetailRelationCard(
    relation: MediaDetailRelation,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    ConnectionRowCard(
        coverUrl = relation.coverUrl,
        palette = relation.palette,
        title = relation.title,
        averageScore = relation.averageScore,
        favourites = relation.favourites,
        topLine = relationLabel(relation.kind, strings),
        metaParts = listOfNotNull(relation.format, relation.year?.toString()),
        onClick = onClick,
    )
}

private fun relationLabel(kind: MediaRelationKind, strings: LanguageStrings): String = when (kind) {
    MediaRelationKind.SEQUEL -> strings.mediaRelationSequel
    MediaRelationKind.PREQUEL -> strings.mediaRelationPrequel
    MediaRelationKind.SIDE_STORY -> strings.mediaRelationSideStory
    MediaRelationKind.SPIN_OFF -> strings.mediaRelationSpinOff
    MediaRelationKind.PARENT -> strings.mediaRelationParent
    MediaRelationKind.ADAPTATION -> strings.mediaRelationAdaptation
    MediaRelationKind.ALTERNATIVE -> strings.mediaRelationAlternative
    MediaRelationKind.SOURCE -> strings.mediaRelationSource
    MediaRelationKind.SUMMARY -> strings.mediaRelationSummary
    MediaRelationKind.CHARACTER -> strings.mediaRelationCharacter
    MediaRelationKind.OTHER -> strings.mediaRelationOther
}

@Composable
private fun MediaDetailStudios(studios: List<String>, strings: LanguageStrings) {
    MediaDetailSection(title = strings.mediaDetailStudiosTitle) {
        Text(
            text = studios.joinToString(" · "),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MediaDetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun ReviewsTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val reviews = stateHolder.reviews
    LaunchedEffect(Unit) {
        stateHolder.loadReviews()
    }
    if (reviews.isEmpty() && !stateHolder.isLoadingReviews) {
        PlatformListMessage(
            title = strings.mediaDetailNoReviewsLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, stateHolder.reviewsHasNextPage) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { last ->
            if (last >= reviews.size - 4 && stateHolder.reviewsHasNextPage && !stateHolder.isLoadingReviews) {
                stateHolder.loadMoreReviews()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(items = reviews, key = { it.id }) { entry ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (entry.reviewerImageUrl != null) {
                                AsyncImage(
                                    model = entry.reviewerImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.reviewerName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = entry.reviewerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = entry.rating,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = entry.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (stateHolder.isLoadingReviews) {
            item { LoadingTile() }
        }
    }
}

@Composable
private fun ThreadsTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val threads = stateHolder.threads
    LaunchedEffect(Unit) {
        stateHolder.loadThreads()
    }
    if (threads.isEmpty() && !stateHolder.isLoadingThreads) {
        PlatformListMessage(
            title = strings.mediaDetailNoThreadsLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, stateHolder.threadsHasNextPage) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { last ->
            if (last >= threads.size - 4 && stateHolder.threadsHasNextPage && !stateHolder.isLoadingThreads) {
                stateHolder.loadMoreThreads()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(items = threads, key = { it.id }) { entry ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "by ${entry.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${entry.replyCount} replies · ${entry.viewCount} views",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (stateHolder.isLoadingThreads) {
            item { LoadingTile() }
        }
    }
}

@Composable
private fun FollowingTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val entries = stateHolder.followingEntries
    LaunchedEffect(Unit) {
        stateHolder.loadFollowing()
    }
    if (entries.isEmpty() && !stateHolder.isLoadingFollowing) {
        PlatformListMessage(
            title = strings.mediaDetailNoFollowingLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }

    if (stateHolder.isLoadingFollowing && entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (entry.userImageUrl != null) {
                        AsyncImage(
                            model = entry.userImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.userName.take(1),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = entry.userName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = entry.progressLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.scoreLabel != "Score: -") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = entry.scoreLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitiesTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val activities = stateHolder.activities
    val currentScope = stateHolder.activitiesScope
    val scopes = remember { listOf("Global", "Following", "Self") }

    LaunchedEffect(currentScope) {
        if (activities.isEmpty()) {
            stateHolder.loadMoreActivities(isFirstPage = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PlatformChips(
            items = scopes,
            selectedItem = currentScope,
            onSelect = { stateHolder.selectActivitiesScope(it) },
            label = { scope ->
                when (scope) {
                    "Global" -> strings.mediaDetailActivityGlobal
                    "Following" -> strings.mediaDetailActivityFollowing
                    else -> strings.mediaDetailActivitySelf
                }
            },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (activities.isEmpty() && !stateHolder.isLoadingActivities) {
            PlatformListMessage(
                title = strings.mediaDetailNoActivitiesLabel,
                tone = PlatformListMessageTone.Neutral,
            )
            return@Column
        }

        val listState = rememberLazyListState()
        LaunchedEffect(listState, stateHolder.activitiesHasNextPage) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }.collect { last ->
                if (last >= activities.size - 4 && stateHolder.activitiesHasNextPage && !stateHolder.isLoadingActivities) {
                    stateHolder.loadMoreActivities()
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = activities, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (entry.userImageUrl != null) {
                                AsyncImage(
                                    model = entry.userImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.userName.take(1).uppercase(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = entry.userName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = entry.actionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stateHolder.detail?.title.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.timeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    painter = PlatformIcons.Like,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${entry.likesCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (stateHolder.isLoadingActivities) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionsTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
    onOpenRelation: (Int) -> Unit,
) {
    var selectedScope by remember { mutableStateOf("Related") }
    val scopes = remember { listOf("Related", "Recommendations") }

    LaunchedEffect(selectedScope) {
        if (selectedScope == "Recommendations") {
            stateHolder.loadRecommendations()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PlatformChips(
            items = scopes,
            selectedItem = selectedScope,
            onSelect = { selectedScope = it },
            label = { scope ->
                if (scope == "Related") strings.mediaDetailRelationsTitle else strings.mediaDetailTabRecommendations
            },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (selectedScope == "Related") {
                val relations = stateHolder.detail?.relations.orEmpty()
                if (relations.isEmpty()) {
                    PlatformListMessage(
                        title = strings.mediaDetailNoRecommendationsLabel,
                        tone = PlatformListMessageTone.Neutral,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = relations, key = { it.mediaId }) { relation ->
                            MediaDetailRelationCard(
                                relation = relation,
                                strings = strings,
                                onClick = { onOpenRelation(relation.mediaId) },
                            )
                        }
                    }
                }
            } else {
                val recommendations = stateHolder.recommendations
                if (recommendations.isEmpty() && !stateHolder.isLoadingRecommendations) {
                    PlatformListMessage(
                        title = strings.mediaDetailNoRecommendationsLabel,
                        tone = PlatformListMessageTone.Neutral,
                    )
                } else if (stateHolder.isLoadingRecommendations && recommendations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = recommendations, key = { it.id }) { entry ->
                            MediaRecommendationCard(
                                entry = entry,
                                strings = strings,
                                onClick = { onOpenRelation(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRecommendationCard(
    entry: MediaRecommendationEntry,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    val countLabel = when {
        entry.episodesCount != null && entry.episodesCount > 0 -> "${entry.episodesCount} eps"
        entry.chaptersCount != null && entry.chaptersCount > 0 -> "${entry.chaptersCount} ch"
        entry.volumesCount != null && entry.volumesCount > 0 -> "${entry.volumesCount} vols"
        else -> null
    }
    ConnectionRowCard(
        coverUrl = entry.coverUrl,
        palette = emptyList(),
        title = entry.title,
        averageScore = entry.averageScore,
        favourites = entry.favouritesCount,
        topLine = null,
        metaParts = listOfNotNull(entry.format, entry.year?.toString(), countLabel),
        onClick = onClick,
    )
}

@Composable
private fun ConnectionRowCard(
    coverUrl: String?,
    palette: List<Color>,
    title: String,
    averageScore: Int?,
    favourites: Int?,
    topLine: String?,
    metaParts: List<String>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PlatformMediaCover(
            coverUrl = coverUrl,
            palette = palette,
            contentDescription = title,
            modifier = Modifier
                .width(PlatformCoverSize.RowPosterWidth)
                .aspectRatio(PlatformCoverSize.PosterAspectRatio),
        ) {
            if (averageScore != null && averageScore > 0) {
                PlatformRatingBadge(averageScore = averageScore)
            }
            if (favourites != null && favourites > 0) {
                PlatformFavouritesBadge(favourites = favourites)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!topLine.isNullOrBlank()) {
                Text(
                    text = topLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
