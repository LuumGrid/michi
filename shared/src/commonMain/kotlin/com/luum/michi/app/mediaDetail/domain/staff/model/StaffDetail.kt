package com.luum.michi.app.mediaDetail.domain.staff.model
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.common.BiographyStat



internal data class StaffDetail(
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
    val birthday: String?,
    val death: String?,
    val yearsActiveStart: Int?,
    val yearsActiveEnd: Int?,
    val homeTown: String?,
    val occupations: String?,
    val bloodType: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: Page<StaffMediaItem>,
    val characters: Page<StaffCharacterItem>,
)

internal data class StaffMediaItem(
    val mediaId: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val staffRole: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
)

internal data class StaffCharacterItem(
    val characterId: Int,
    val name: String,
    val image: CoverSizes?,
    val mediaTitle: String?,
    /** The media this role belongs to; identity for dedup (a character in two works is two rows). */
    val mediaId: Int? = null,
)

/** Canonical AniList page for this staff member. */
internal fun StaffDetail.aniListUrl(): String = "https://anilist.co/staff/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StaffDetail.shareText(): String = "$name\n${aniListUrl()}"
