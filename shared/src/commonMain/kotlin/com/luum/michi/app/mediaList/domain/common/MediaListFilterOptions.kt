package com.luum.michi.app.mediaList.domain.common

import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.FilterOption

/** Static genre list, mirrored from Explore (kept local: features must not import each other). */
internal fun mediaListGenres(): List<String> = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
    "Slice of Life", "Sports", "Supernatural", "Thriller",
)

/**
 * Selectable years: next year first (covers next season falling in January),
 * then backwards, with the classic curated tail. Mirrored from Explore.
 */
internal fun mediaListYears(): List<Int?> {
    val currentYear = currentSeasonAndYear().year
    return listOf(null) + ((currentYear + 1) downTo 2011) + listOf(2010, 2005, 2000)
}

internal fun mediaListSeasonOptions(strings: LanguageStrings): List<FilterOption> {
    val labels = listOf(
        strings.seasonWinterLabel,
        strings.seasonSpringLabel,
        strings.seasonSummerLabel,
        strings.seasonFallLabel,
    )
    val ids = listOf("WINTER", "SPRING", "SUMMER", "FALL")
    return listOf(FilterOption("any", strings.exploreAnySeasonLabel)) +
        ids.zip(labels) { id, label -> FilterOption(id, label) }
}

/**
 * Opciones de formato por tab (jerga AniList sin traducir, igual que en Explore):
 * anime excluye MANGA/NOVEL/ONE_SHOT, manga solo lleva esos tres.
 */
internal fun mediaListFormatOptions(isAnimeTab: Boolean): List<FilterOption> =
    MediaFormat.entries
        .filter {
            if (isAnimeTab) {
                it != MediaFormat.MANGA && it != MediaFormat.NOVEL &&
                    it != MediaFormat.ONE_SHOT && it != MediaFormat.UNKNOWN
            } else {
                it == MediaFormat.MANGA || it == MediaFormat.NOVEL || it == MediaFormat.ONE_SHOT
            }
        }
        .map { FilterOption(it.name, it.label()) }

/**
 * Client-side predicate shared by both lists (the whole collection is already
 * loaded, so season/genre/year filter in memory, like Explore's server-side
 * `season`/`genre_in`/year but over local entries). Genre matches if ANY of
 * the entry's genres is selected.
 */
internal fun matchesMediaListFilters(
    season: MediaSeason?,
    genres: List<String>,
    seasonYear: Int?,
    formatName: String,
    filterSeason: MediaSeason?,
    filterGenres: List<String>,
    filterYear: Int?,
    filterFormats: List<String>,
): Boolean =
    (filterSeason == null || season == filterSeason) &&
        (filterYear == null || seasonYear == filterYear) &&
        (filterGenres.isEmpty() || genres.any { it in filterGenres }) &&
        (filterFormats.isEmpty() || formatName in filterFormats)
