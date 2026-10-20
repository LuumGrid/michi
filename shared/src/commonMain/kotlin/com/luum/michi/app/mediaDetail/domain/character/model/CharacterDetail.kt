package com.luum.michi.app.mediaDetail.domain.character.model
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.common.BiographyStat



internal data class CharacterDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val image: CoverSizes?,
    val descriptionPlain: String,
    /** Hidden spoiler ranges over [descriptionPlain] (inclusive, empty when none). */
    val descriptionSpoilers: List<IntRange> = emptyList(),
    val stats: List<BiographyStat>,
    val links: List<ExternalLink>,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val birthday: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: Page<CharacterMediaItem>,
)

internal data class CharacterMediaItem(
    val mediaId: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val role: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    val voiceActorName: String?,
    val voiceActorImage: CoverSizes?,
    val voiceActorLanguage: String?,
    val voiceActorId: Int?,
)

/** Canonical AniList page for this character. */
internal fun CharacterDetail.aniListUrl(): String = "https://anilist.co/character/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun CharacterDetail.shareText(): String = "$name\n${aniListUrl()}"
