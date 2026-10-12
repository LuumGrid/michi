package com.luum.michi.app.mediaDetail.domain.character.model

import com.luum.michi.app.core.language.domain.LanguageStrings


internal data class CharacterDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val alternativeSpoilerNames: List<String>,
    val imageUrl: String?,
    val descriptionPlain: String,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val birthday: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: CharacterMediaPage,
)

internal data class CharacterMediaItem(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val paletteHex: String?,
    val role: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    val voiceActorName: String?,
    val voiceActorImageUrl: String?,
    val voiceActorLanguage: String?,
    val voiceActorId: Int?,
)

internal data class CharacterMediaPage(
    val items: List<CharacterMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class CharacterMediaSort(val apiValue: String) {
    POPULARITY("POPULARITY_DESC"),
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
}

/** Localized chip label for the appearances sort (strings live in the tables). */
internal fun CharacterMediaSort.label(strings: LanguageStrings): String = when (this) {
    CharacterMediaSort.POPULARITY -> strings.sortByPopularity
    CharacterMediaSort.NEWEST -> strings.sortByNewest
    CharacterMediaSort.OLDEST -> strings.sortByOldest
    CharacterMediaSort.FAVOURITES -> strings.sortByFavourites
    CharacterMediaSort.SCORE -> strings.sortByScore
}

/**
 * Client-side display ordering. The list is fetched from the server by
 * popularity, so switching chips never re-queries: score/year are already on
 * every item. POPULARITY/FAVOURITES keep the server order.
 */
internal fun List<CharacterMediaItem>.sortedFor(sort: CharacterMediaSort): List<CharacterMediaItem> =
    when (sort) {
        CharacterMediaSort.SCORE -> sortedByDescending { it.averageScore ?: -1 }
        CharacterMediaSort.NEWEST -> sortedByDescending { it.year ?: -1 }
        CharacterMediaSort.OLDEST -> sortedBy { it.year ?: Int.MAX_VALUE }
        else -> this
    }

/** Canonical AniList page for this character. */
internal fun CharacterDetail.aniListUrl(): String = "https://anilist.co/character/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun CharacterDetail.shareText(): String = "$name\n${aniListUrl()}"
