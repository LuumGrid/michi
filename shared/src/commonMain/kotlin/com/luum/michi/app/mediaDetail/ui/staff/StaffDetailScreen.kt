package com.luum.michi.app.mediaDetail.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.ui.components.DetailLoadState
import com.luum.michi.app.mediaDetail.ui.components.DetailPageColumn
import com.luum.michi.app.mediaDetail.ui.components.InfoPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoEntry
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.components.addInfo
import com.luum.michi.app.mediaDetail.ui.components.alternativeNamesEntry
import com.luum.michi.app.mediaDetail.ui.components.personBiography
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.model.RailPageSize

/**
 * Staff detail page: person hero (portrait + identity, with the occupations as
 * chips inside it) + description + information panel + staff roles rail +
 * characters rail (own pagination). Reached from a media staff rail via
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
    DetailLoadState(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        error = holder.error,
        actionError = holder.actionError,
        onRetry = onRetry,
        onDismissActionError = { holder.clearActionError() },
        strings = strings,
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
    DetailPageColumn(bottomPadding = bottomPadding, modifier = modifier) {
        item(key = "hero") {
            MediaDetailPersonHero(
                name = detail.name,
                nativeName = detail.nativeName,
                image = detail.image,
                favourites = detail.favourites,
                strings = strings,
                // The occupations live in the hero column, not in their own row.
                chips = staffChips(detail),
            )
        }
        personBiography(
            title = strings.mediaDetailBiographyTitle,
            text = detail.descriptionPlain,
            stats = detail.stats,
            links = detail.links,
            strings = strings,
            onOpenUrl = onOpenUrl,
        )
        // The cells are built here so an empty panel is not emitted at all: an
        // item with nothing to paint still leaves its spacing behind (the gap
        // after the links on a staff member with no biography).
        val yearsActive = detail.yearsActiveStart?.let { start ->
            "$start–${detail.yearsActiveEnd ?: strings.yearsActivePresent}"
        }
        val informationEntries = buildList {
            addInfo(strings.mediaDetailGenderLabel, detail.gender)
            addInfo(strings.mediaDetailAgeLabel, detail.age)
            addInfo(strings.mediaDetailBirthdayLabel, detail.birthday)
            addInfo(strings.mediaDetailDeathLabel, detail.death)
            yearsActive?.let { add(MediaDetailInfoEntry(strings.mediaDetailYearsActiveLabel, it)) }
            addInfo(strings.mediaDetailHometownLabel, detail.homeTown)
            addInfo(strings.mediaDetailBloodTypeLabel, detail.bloodType)
        }
        val informationWideEntries = listOfNotNull(
            alternativeNamesEntry(detail.alternativeNames, strings.mediaDetailAlternativeNamesLabel),
        )
        if (informationEntries.isNotEmpty() || informationWideEntries.isNotEmpty()) {
            item(key = "information") {
                InfoPanel(
                    entries = informationEntries,
                    wideEntries = informationWideEntries,
                    strings = strings,
                )
            }
        }
        // Empty rails are not painted at all (a header with "No media" reads as
        // a broken section).
        if (holder.media.items.isNotEmpty()) {
            item(key = "roles") {
                RolesSection(
                    holder = holder,
                    strings = strings,
                    onOpenMedia = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.STAFF_MEDIA) },
                )
            }
        }
        if (holder.characters.items.isNotEmpty()) {
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
private fun RolesSection(
    holder: StaffDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
    onViewAll: () -> Unit,
) {
    MediaRail(
        title = strings.mediaDetailRolesTitle,
        items = holder.media.items.take(RailPageSize).map { it.toRailItem() },
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
        items = holder.characters.items.take(RailPageSize).map { it.toRailItem() },
        onSelect = onOpenCharacter,
        onViewAll = onViewAll,
        viewAllLabel = strings.viewAllAction,
    )
}

