package com.luum.michi.app.mediaDetail.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailExpandableText
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoEntry
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoGrid
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkSection
import com.luum.michi.app.mediaDetail.ui.components.toLinkItem

/**
 * Staff detail page: person hero + framed occupations chips + description +
 * information panel + staff roles rail + characters rail (own pagination).
 * Reached from a media staff rail via
 * [com.luum.michi.app.root.state.State.openStaff].
 */
@Composable
internal fun StaffDetailScreen(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    LoadStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
    ) {
        if (detail != null) {
            StaffDetailContent(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
                onOpenUrl = onOpenUrl,
                onOpenMedia = onOpenMedia,
                onOpenCharacter = onOpenCharacter,
                onViewAll = onViewAll,
            )
        }
    }
}

@Composable
private fun StaffDetailContent(
    detail: StaffDetail,
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenUrl: (String) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit,
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
                image = detail.image,
                favourites = detail.favourites,
                strings = strings,
            )
        }
        val chips = staffChips(detail)
        if (chips.isNotEmpty()) {
            // The occupations are the whole section: one full-bleed chip row
            // under the hero, no panel and no title.
            item(key = "occupations") {
                ChipRail(
                    tabs = chips.map { ChipTab(value = it, label = it) },
                    selected = null,
                    onSelect = {},
                    interactive = false,
                )
            }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "description") {
                MediaDetailExpandableText(
                    title = strings.mediaDetailBiographyTitle,
                    text = detail.descriptionPlain,
                    strings = strings,
                )
            }
        }
        if (detail.links.isNotEmpty()) {
            item(key = "links") {
                MediaDetailLinkSection(
                    links = detail.links.map { it.toLinkItem(strings) },
                    title = strings.mediaDetailExternalLinksTitle,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
        // The cells are built here so an empty panel is not emitted at all: an
        // item with nothing to paint still leaves its spacing behind (the gap
        // after the links on a staff member with no biography).
        val yearsActive = detail.yearsActiveStart?.let { start ->
            "$start–${detail.yearsActiveEnd ?: strings.yearsActivePresent}"
        }
        val informationEntries = buildList {
            detail.gender?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailGenderLabel, it)) }
            detail.age?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailAgeLabel, it)) }
            detail.birthday?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailBirthdayLabel, it)) }
            detail.death?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailDeathLabel, it)) }
            yearsActive?.let { add(MediaDetailInfoEntry(strings.mediaDetailYearsActiveLabel, it)) }
            detail.homeTown?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailHometownLabel, it)) }
            detail.bloodType?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailBloodTypeLabel, it)) }
        }
        val informationWideEntries = detail.alternativeNames
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(MediaDetailInfoEntry(strings.mediaDetailAlternativeNamesLabel, it.joinToString(", "))) }
            .orEmpty()
        if (informationEntries.isNotEmpty() || informationWideEntries.isNotEmpty()) {
            item(key = "information") {
                StaffInformationPanel(
                    entries = informationEntries,
                    wideEntries = informationWideEntries,
                    strings = strings,
                )
            }
        }
        // Empty rails are not painted at all (a header with "No media" reads as
        // a broken section).
        if (holder.mediaItems.isNotEmpty()) {
            item(key = "roles") {
                RolesSection(
                    holder = holder,
                    strings = strings,
                    onOpenMedia = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.STAFF_MEDIA) },
                )
            }
        }
        if (holder.characterItems.isNotEmpty()) {
            item(key = "characters") {
                CharactersSection(
                    holder = holder,
                    strings = strings,
                    onOpenCharacter = onOpenCharacter,
                    onViewAll = { onViewAll(DetailListKind.STAFF_CHARACTERS) },
                )
            }
        }
    }
}

/** Occupations, if any (favourites live in the hero). */
private fun staffChips(detail: StaffDetail): List<String> =
    buildList {
        detail.occupations
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.forEach { add(it) }
    }

@Composable
private fun StaffInformationPanel(
    entries: List<MediaDetailInfoEntry>,
    wideEntries: List<MediaDetailInfoEntry>,
    strings: LanguageStrings,
) {
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        MediaDetailInfoGrid(entries = entries, wideEntries = wideEntries)
    }
}

@Composable
private fun RolesSection(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
    onViewAll: () -> Unit,
) {
    MediaRail(
        title = strings.mediaDetailRolesTitle,
        items = holder.mediaItems.take(RailPageSize).map { it.toRailItem() },
        onSelect = onOpenMedia,
        onViewAll = onViewAll,
        viewAllLabel = strings.viewAllAction,
    )
}

@Composable
private fun CharactersSection(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenCharacter: (Int) -> Unit,
    onViewAll: () -> Unit,
) {
    MediaRail(
        title = strings.mediaDetailCharactersTitle,
        items = holder.characterItems.take(RailPageSize).map { it.toRailItem() },
        onSelect = onOpenCharacter,
        onViewAll = onViewAll,
        viewAllLabel = strings.viewAllAction,
    )
}

