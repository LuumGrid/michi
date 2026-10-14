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
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoRow
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailStateContainer
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.network.domain.RailPageSize

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
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    MediaDetailStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        error = holder.error,
        strings = strings,
        onRetry = onRetry,
        modifier = modifier,
    ) {
        if (detail != null) {
            StaffDetailContent(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
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
            item(key = "occupations") {
                MediaDetailPanel {
                    SectionHeader(strings.mediaDetailOccupationsLabel)
                    Spacer(modifier = Modifier.height(12.dp))
                    ChipRail(
                        tabs = chips.map { ChipTab(value = it, label = it) },
                        selected = null,
                        onSelect = {},
                        contentPadding = PaddingValues(0.dp),
                    )
                }
            }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "description") {
                MediaDetailExpandableText(
                    title = strings.mediaDetailDescriptionTitle,
                    text = detail.descriptionPlain,
                    strings = strings,
                )
            }
        }
        item(key = "information") {
            StaffInformationPanel(detail = detail, strings = strings)
        }
        item(key = "roles") {
            RolesSection(
                holder = holder,
                strings = strings,
                onOpenMedia = onOpenMedia,
                onViewAll = { onViewAll(DetailListKind.STAFF_MEDIA) },
            )
        }
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
private fun StaffInformationPanel(detail: StaffDetail, strings: LanguageStrings) {
    val yearsActive = detail.yearsActiveStart?.let { start ->
        "$start–${detail.yearsActiveEnd ?: strings.yearsActivePresent}"
    }
    val rows = buildList {
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
    }
    if (rows.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { (label, value) -> MediaDetailInfoRow(label = label, value = value) }
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
        emptyLabel = strings.mediaDetailNoMediaLabel.takeIf { !holder.isLoading },
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
        emptyLabel = strings.mediaDetailNoCharactersLabel.takeIf { !holder.isLoading },
    )
}

