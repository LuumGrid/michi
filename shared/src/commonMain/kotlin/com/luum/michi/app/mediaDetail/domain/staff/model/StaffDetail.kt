package com.luum.michi.app.mediaDetail.domain.staff.model
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.model.PersonLink



internal data class StaffDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val image: CoverSizes?,
    val descriptionPlain: String,
    val links: List<PersonLink>,
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
    val media: StaffMediaPage,
    val characters: StaffCharacterPage,
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

internal data class StaffMediaPage(
    val items: List<StaffMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class StaffCharacterItem(
    val characterId: Int,
    val name: String,
    val image: CoverSizes?,
    val mediaTitle: String?,
)

internal data class StaffCharacterPage(
    val items: List<StaffCharacterItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

/** Canonical AniList page for this staff member. */
internal fun StaffDetail.aniListUrl(): String = "https://anilist.co/staff/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StaffDetail.shareText(): String = "$name\n${aniListUrl()}"
