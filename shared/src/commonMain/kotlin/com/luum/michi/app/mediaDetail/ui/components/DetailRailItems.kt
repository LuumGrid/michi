package com.luum.michi.app.mediaDetail.ui.components

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.catalogMetaLine
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.media.domain.label as coreModelLabel
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.domain.media.model.label
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.PersonRailItem
import com.luum.michi.app.ui.components.PersonRailPerson
import com.luum.michi.app.ui.components.scoreBadge

/**
 * Domain-entry → [MediaRailItem] mappers shared by the detail rails and the
 * "View all" list screen, so both render the exact same cards.
 */

internal fun MediaCharacterEntry.toRailItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = characterId,
    title = name,
    image = image,
    subtitle = characterRoleLabel(role.name, strings),
    key = edgeKey,
)

internal fun MediaStaffEntry.toRailItem(): MediaRailItem = MediaRailItem(
    id = staffId,
    title = name,
    image = image,
    subtitle = role,
    key = edgeKey,
)

internal fun StaffMediaItem.toRailItem(): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    image = cover,
    subtitle = staffRole?.takeIf { it.isNotBlank() },
    placeholderHex = paletteHex,
)

internal fun StaffCharacterItem.toRailItem(): MediaRailItem = MediaRailItem(
    id = characterId,
    title = name,
    image = image,
    subtitle = mediaTitle?.takeIf { it.isNotBlank() },
)

/** The character card: role under the name, plus the Japanese actor when there is one. */
internal fun MediaCharacterEntry.toPersonItem(strings: LanguageStrings): PersonRailItem = PersonRailItem(
    primary = PersonRailPerson(
        id = characterId,
        name = name,
        image = image,
        subtitle = characterRoleLabel(role.name, strings),
    ),
    secondary = voiceActor?.let { actor ->
        PersonRailPerson(
            id = actor.staffId,
            name = actor.name,
            image = actor.image,
            subtitle = voiceLanguageLabel(actor.language, strings),
        )
    },
)

/** The staff card: the role under the name, when AniList sent one. */
internal fun MediaStaffEntry.toPersonItem(): PersonRailItem = PersonRailItem(
    primary = PersonRailPerson(
        id = staffId,
        name = name,
        image = image,
        subtitle = role?.takeIf { it.isNotBlank() },
    ),
)

/** The staff-detail character card: the work under the character's name. */
internal fun StaffCharacterItem.toPersonItem(): PersonRailItem = PersonRailItem(
    primary = PersonRailPerson(
        id = characterId,
        name = name,
        image = image,
        subtitle = mediaTitle?.takeIf { it.isNotBlank() },
    ),
)

/**
 * One voice-actor card, or null when the cast edge has no reachable actor. The
 * subtitle is the anime (the language picker already states the language).
 */
internal fun CharacterMediaItem.toVoiceActorItem(): PersonRailItem? {
    val actorId = voiceActorId ?: return null
    val actorName = voiceActorName?.takeIf { it.isNotBlank() } ?: return null
    return PersonRailItem(
        primary = PersonRailPerson(
            id = actorId,
            name = actorName,
            image = voiceActorImage,
            subtitle = title.takeIf { it.isNotBlank() },
        ),
    )
}

/** Related media card: catalog meta as the subtitle and the relation kind as a chip. */
internal fun MediaDetailRelation.toCardRailItem(
    strings: LanguageStrings,
    isManga: Boolean,
): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    image = cover,
    subtitle = catalogMeta(format, season, year, workStatus, strings, isManga),
    tag = kind.label(strings),
)

/** Recommendation card: catalog meta as the subtitle and the score as its chip. */
internal fun MediaRecommendationEntry.toCardRailItem(
    strings: LanguageStrings,
    isManga: Boolean,
): MediaRailItem = MediaRailItem(
    id = id,
    title = title,
    image = cover,
    meta = catalogMeta(format, season, year, workStatus, strings, isManga),
    stats = listOfNotNull(scoreBadge(averageScore)),
)

/** Appearance card on a character page: the role under the work's title. */
internal fun CharacterMediaItem.toAppearanceCardItem(strings: LanguageStrings): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    image = cover,
    subtitle = characterRoleLabel(role, strings),
    placeholderHex = paletteHex,
)

/** Localized role label; unknown values fall through to null. */
internal fun characterRoleLabel(role: String?, strings: LanguageStrings): String? = when (role) {
    "MAIN" -> strings.mediaDetailCharacterRoleMain
    "SUPPORTING" -> strings.mediaDetailCharacterRoleSupporting
    "BACKGROUND" -> strings.mediaDetailCharacterRoleBackground
    else -> null
}

/** `TV · Summer 2026 · Airing` — the lists/calendar meta line. `isManga`
 *  picks the manga status wording (Publishing/Finished) so a manga detail
 *  reads like the manga list, not the anime one. */
private fun catalogMeta(
    format: String?,
    season: MediaSeason?,
    year: Int?,
    status: MediaWorkStatus?,
    strings: LanguageStrings,
    isManga: Boolean,
): String? = catalogMetaLine(
    format = format,
    season = season?.coreModelLabel(strings),
    year = year,
    status = status?.coreModelLabel(strings, isManga),
).ifBlank { null }
