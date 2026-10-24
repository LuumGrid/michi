package com.luum.michi.app.discover.domain.model

import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.media.domain.parseMediaSeason
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort

/**
 * The catalog criteria behind a Dashboard rail (or a genre page). It is pure
 * data so the same filter drives the rail's query (statically written) and the
 * "View all" catalog grid. [defaultSortOption]/[defaultSortOrder] are the rail's
 * sort, which the catalog grid opens on (the server `MediaSort` is derived with
 * `toMediaSort`).
 */
internal data class MediaCatalogFilter(
    val type: MediaType,
    val genres: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDateGreater: Int? = null,
    val popularityGreater: Int? = null,
    val defaultSortOption: UserListSort = UserListSort.POPULARITY,
    val defaultSortOrder: UserListOrder = UserListOrder.DESCENDING,
)

/** A genre page is just the catalog filtered by name (type + `genre_in`). */
internal fun genreCatalogFilter(name: String, type: MediaType): MediaCatalogFilter =
    MediaCatalogFilter(type = type, genres = listOf(name))

/** The season+year this filter currently selects, or null when it is not a season catalog. */
internal fun MediaCatalogFilter.selectedSeason(): MediaSeasonYear? {
    val season = parseMediaSeason(season) ?: return null
    val year = seasonYear ?: return null
    return MediaSeasonYear(season, year)
}
