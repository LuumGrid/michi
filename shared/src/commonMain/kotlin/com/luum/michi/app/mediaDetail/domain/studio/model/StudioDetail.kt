package com.luum.michi.app.mediaDetail.domain.studio.model

import com.luum.michi.app.core.language.domain.LanguageStrings


internal data class StudioDetail(
    val id: Int,
    val name: String,
    val isAnimeStudio: Boolean,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: StudioMediaPage,
)

internal data class StudioMediaItem(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val paletteHex: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    /** Raw AniList type: "ANIME" / "MANGA". */
    val type: String? = null,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
)

internal data class StudioMediaPage(
    val items: List<StudioMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class StudioMediaSort(val apiValue: String) {
    POPULARITY("POPULARITY_DESC"),
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
}

/** Localized chip label for the studio media sort (strings live in the tables). */
internal fun StudioMediaSort.label(strings: LanguageStrings): String = when (this) {
    StudioMediaSort.POPULARITY -> strings.sortByPopularity
    StudioMediaSort.NEWEST -> strings.sortByNewest
    StudioMediaSort.OLDEST -> strings.sortByOldest
    StudioMediaSort.FAVOURITES -> strings.sortByFavourites
    StudioMediaSort.SCORE -> strings.sortByScore
}

/**
 * Client-side display ordering over the server-popularity list; the grid
 * filters and sorts without ever re-querying. POPULARITY/FAVOURITES keep the
 * server order.
 */
internal fun List<StudioMediaItem>.sortedFor(sort: StudioMediaSort): List<StudioMediaItem> =
    when (sort) {
        StudioMediaSort.SCORE -> sortedByDescending { it.averageScore ?: -1 }
        StudioMediaSort.NEWEST -> sortedByDescending { it.year ?: -1 }
        StudioMediaSort.OLDEST -> sortedBy { it.year ?: Int.MAX_VALUE }
        else -> this
    }

/** Canonical AniList page for this studio. */
internal fun StudioDetail.aniListUrl(): String = "https://anilist.co/studio/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StudioDetail.shareText(): String = "$name\n${aniListUrl()}"
