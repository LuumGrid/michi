package com.luum.michi.app.mediaDetail.ui.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaSort
import com.luum.michi.app.mediaDetail.domain.staff.model.label
import com.luum.michi.app.mediaDetail.domain.staff.model.sortedFor
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailChipRow
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailSectionHeader
import com.luum.michi.app.mediaDetail.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.MediaRailItem
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.language.Strings

/**
 * Staff detail page: person hero + person chips + description + information
 * panel + anime/manga staff roles split (own local sort) + characters rail
 * (own pagination). Reached from a media staff rail via
 * [com.luum.michi.app.root.state.State.openStaff].
 */
@Composable
internal fun StaffDetailScreen(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
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
                StaffDetailContent(
                    detail = detail,
                    holder = holder,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onOpenMedia = onOpenMedia,
                    onOpenCharacter = onOpenCharacter,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun StaffDetailContent(
    detail: StaffDetail,
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "hero") {
            MediaDetailPersonHero(
                name = detail.name,
                nativeName = detail.nativeName,
                imageUrl = detail.imageUrl,
                favourites = detail.favourites,
                strings = strings,
            )
        }
        val chips = staffChips(detail, strings)
        if (chips.isNotEmpty()) {
            item(key = "chips") { MediaDetailChipRow(chips = chips) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "description") {
                PersonDescription(text = detail.descriptionPlain, strings = strings)
            }
        }
        item(key = "information") {
            StaffInformationPanel(detail = detail, strings = strings)
        }
        item(key = "roles") {
            RolesSection(holder = holder, strings = strings, onOpenMedia = onOpenMedia)
        }
        item(key = "characters") {
            CharactersSection(holder = holder, strings = strings, onOpenCharacter = onOpenCharacter)
        }
    }
}

/** Occupations (if any) + favourites. */
private fun staffChips(detail: StaffDetail, strings: LanguageStrings): List<String> =
    buildList {
        detail.favourites?.let { add("$it ${strings.favoritesLabel}") }
        detail.occupations
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.forEach { add(it) }
    }

@Composable
private fun StaffInformationPanel(detail: StaffDetail, strings: LanguageStrings) {
    val yearsActive = detail.yearsActiveStart?.let { start ->
        "$start–${detail.yearsActiveEnd ?: strings.yearsActivePresent}"
    }
    val rows = buildList {
        detail.nativeName?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailJapaneseLabel to it) }
        detail.gender?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailGenderLabel to it) }
        detail.age?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailAgeLabel to it) }
        detail.birthday?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBirthdayLabel to it) }
        detail.death?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailDeathLabel to it) }
        yearsActive?.let { add(strings.mediaDetailYearsActiveLabel to it) }
        detail.homeTown?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailHometownLabel to it) }
        detail.occupations?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailOccupationsLabel to it) }
        detail.bloodType?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBloodTypeLabel to it) }
        detail.alternativeNames
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(strings.mediaDetailSynonymsLabel to it.joinToString(", ")) }
        detail.favourites?.let { add(strings.favoritesLabel to it.toString()) }
    }
    if (rows.isEmpty()) return
    MediaDetailPanel {
        MediaDetailSectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { (label, value) -> InfoRow(label = label, value = value) }
    }
}

@Composable
private fun RolesSection(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
) {
    // `type == null` is treated as anime (older/staff-only edges may omit it).
    val anime = holder.mediaItems.filter { it.type != "MANGA" }.sortedFor(holder.sort)
    val manga = holder.mediaItems.filter { it.type == "MANGA" }.sortedFor(holder.sort)
    val loadMore = if (holder.mediaHasNextPage && !holder.isLoadingMoreMedia) {
        { holder.loadMoreMedia() }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailRolesTitle,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChipRail(
            tabs = StaffMediaSort.entries.map { ChipTab(value = it, label = it.label(strings)) },
            selected = holder.sort,
            onSelect = holder::changeSort,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (holder.mediaItems.isEmpty() && !holder.isLoading) {
            Text(
                text = strings.mediaDetailNoMediaLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            return@Column
        }
        if (anime.isNotEmpty()) {
            StaffRoleGroup(
                title = strings.mediaDetailAnimeRolesTitle,
                items = anime,
                onOpenMedia = onOpenMedia,
                // Only the last rendered group owns the shared load-more footer.
                onLoadMore = if (manga.isEmpty()) loadMore else null,
                loadMoreLabel = strings.mediaDetailLoadMoreAction,
            )
        }
        if (manga.isNotEmpty()) {
            StaffRoleGroup(
                title = strings.mediaDetailMangaRolesTitle,
                items = manga,
                onOpenMedia = onOpenMedia,
                onLoadMore = loadMore,
                loadMoreLabel = strings.mediaDetailLoadMoreAction,
            )
        }
    }
}

@Composable
private fun StaffRoleGroup(
    title: String,
    items: List<StaffMediaItem>,
    onOpenMedia: (Int) -> Unit,
    onLoadMore: (() -> Unit)?,
    loadMoreLabel: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        MediaRail(
            title = null,
            items = items.map { item ->
                MediaRailItem(
                    id = item.mediaId,
                    title = item.title,
                    imageUrl = item.coverUrl,
                    subtitle = item.staffRole?.takeIf { it.isNotBlank() },
                )
            },
            onSelect = onOpenMedia,
            onLoadMore = onLoadMore,
            loadMoreLabel = loadMoreLabel,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CharactersSection(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenCharacter: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailCharactersTitle,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (holder.characterItems.isEmpty() && !holder.isLoading) {
            Text(
                text = strings.mediaDetailNoCharactersLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            MediaRail(
                title = null,
                items = holder.characterItems.map { item ->
                    MediaRailItem(
                        id = item.characterId,
                        title = item.name,
                        imageUrl = item.imageUrl,
                        subtitle = item.mediaTitle?.takeIf { it.isNotBlank() },
                    )
                },
                onSelect = onOpenCharacter,
                onLoadMore = if (holder.charactersHasNextPage && !holder.isLoadingMoreCharacters) {
                    { holder.loadMoreCharacters() }
                } else {
                    null
                },
                loadMoreLabel = strings.mediaDetailLoadMoreAction,
            )
        }
    }
}

@Composable
private fun PersonDescription(text: String, strings: LanguageStrings) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = strings.mediaDetailDescriptionTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
