package com.luum.michi.app.mediaDetail.domain.staff.model

import com.luum.michi.app.core.language.domain.LanguageStrings


internal data class StaffDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val imageUrl: String?,
    val descriptionPlain: String,
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
    val coverUrl: String?,
    val paletteHex: String?,
    val staffRole: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    /** Raw AniList type: "ANIME" / "MANGA" (null treated as anime). */
    val type: String? = null,
)

internal data class StaffMediaPage(
    val items: List<StaffMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class StaffCharacterItem(
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val mediaTitle: String?,
)

internal data class StaffCharacterPage(
    val items: List<StaffCharacterItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class StaffMediaSort(val apiValue: String) {
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    POPULARITY("POPULARITY_DESC"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
    TITLE("TITLE_ROMAJI"),
}

/** Localized chip label for the roles sort (strings live in the tables). */
internal fun StaffMediaSort.label(strings: LanguageStrings): String = when (this) {
    StaffMediaSort.NEWEST -> strings.sortByNewest
    StaffMediaSort.OLDEST -> strings.sortByOldest
    StaffMediaSort.POPULARITY -> strings.sortByPopularity
    StaffMediaSort.FAVOURITES -> strings.sortByFavourites
    StaffMediaSort.SCORE -> strings.sortByScore
    StaffMediaSort.TITLE -> strings.sortTitle
}

/**
 * Client-side display ordering. The list is fetched from the server by
 * popularity, so switching chips never re-queries: score/year/title are
 * already on every item. POPULARITY/FAVOURITES keep the server order.
 */
internal fun List<StaffMediaItem>.sortedFor(sort: StaffMediaSort): List<StaffMediaItem> =
    when (sort) {
        StaffMediaSort.SCORE -> sortedByDescending { it.averageScore ?: -1 }
        StaffMediaSort.NEWEST -> sortedByDescending { it.year ?: -1 }
        StaffMediaSort.OLDEST -> sortedBy { it.year ?: Int.MAX_VALUE }
        StaffMediaSort.TITLE -> sortedBy { it.title.lowercase() }
        else -> this
    }

/** Canonical AniList page for this staff member. */
internal fun StaffDetail.aniListUrl(): String = "https://anilist.co/staff/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StaffDetail.shareText(): String = "$name\n${aniListUrl()}"
