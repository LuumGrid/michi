package com.luum.michi.app.mediaDetail.ui.components

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.model.label as coreModelLabel
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.domain.media.model.label
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem

/**
 * Domain-entry → [MediaRailItem] mappers shared by the detail rails and the
 * "View all" list screen, so both render the exact same cards.
 */

internal fun MediaCharacterEntry.toRailItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = characterId,
    title = name,
    imageUrl = imageUrl,
    subtitle = characterRoleLabel(role.name, strings),
    key = edgeKey,
)

internal fun MediaStaffEntry.toRailItem(): MediaRailItem = MediaRailItem(
    id = staffId,
    title = name,
    imageUrl = imageUrl,
    subtitle = role,
    key = edgeKey,
)

internal fun MediaDetailRelation.toRailItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    imageUrl = coverUrl,
    subtitle = kind.label(strings),
    meta = catalogMeta(format, season, year, workStatus, strings),
)

internal fun MediaRecommendationEntry.toRailItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = id,
    title = title,
    imageUrl = coverUrl,
    meta = catalogMeta(format, season, year, workStatus, strings),
)

internal fun CharacterMediaItem.toRailItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    imageUrl = coverUrl,
    subtitle = listOfNotNull(
        characterRoleLabel(role, strings),
        voiceActorName,
    ).joinToString(" · ").takeIf { it.isNotBlank() },
)

internal fun StaffMediaItem.toRailItem(): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    imageUrl = coverUrl,
    subtitle = staffRole?.takeIf { it.isNotBlank() },
)

internal fun StaffCharacterItem.toRailItem(): MediaRailItem = MediaRailItem(
    id = characterId,
    title = name,
    imageUrl = imageUrl,
    subtitle = mediaTitle?.takeIf { it.isNotBlank() },
)

/** Localized role label; unknown values fall through to null. */
internal fun characterRoleLabel(role: String?, strings: LanguageStrings): String? = when (role) {
    "MAIN" -> strings.mediaDetailCharacterRoleMain
    "SUPPORTING" -> strings.mediaDetailCharacterRoleSupporting
    "BACKGROUND" -> strings.mediaDetailCharacterRoleBackground
    else -> null
}

/** `TV · Summer 2026 · Airing` — the lists/calendar meta line (manga status
 *  label reads as anime, matching the calendar cards). */
private fun catalogMeta(
    format: String?,
    season: MediaSeason?,
    year: Int?,
    status: MediaWorkStatus?,
    strings: LanguageStrings,
): String? {
    val seasonYear = season?.let { s ->
        year?.let { "${s.coreModelLabel(strings)} $it" } ?: s.coreModelLabel(strings)
    } ?: year?.toString()
    return listOfNotNull(format, seasonYear, status?.coreModelLabel(strings, isManga = false))
        .joinToString(" · ")
        .ifBlank { null }
}
