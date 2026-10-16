package com.luum.michi.app.mediaList.domain.common

import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort

/**
 * Shared rail assembly for both user lists: pick the section, apply the 18+
 * setting and the client-side filters, then sort. The progress and next-airing
 * comparators arrive per list (episodes vs chapters, airing date vs list order
 * genuinely differ).
 */
internal fun <T : SortableMediaListEntry> List<T>.sectionEntries(
    sectionPredicate: (T) -> Boolean,
    hideAdult: Boolean,
    filterSeason: MediaSeason?,
    filterGenres: List<String>,
    filterYear: Int?,
    filterFormats: List<String>,
    sort: UserListSort,
    order: UserListOrder,
    progressOrder: Comparator<T>,
    airingOrder: Comparator<T>,
): List<T> {
    val filtered = filter(sectionPredicate)
        // Global 18+ setting (not a sheet filter): applied here so it stays
        // fresh per composition with no holder state and no re-fetch.
        .filter { !hideAdult || !it.isAdult }
        .filter {
            matchesMediaListFilters(
                season = it.season,
                genres = it.genres,
                seasonYear = it.seasonYear,
                formatName = it.format.name,
                filterSeason = filterSeason,
                filterGenres = filterGenres,
                filterYear = filterYear,
                filterFormats = filterFormats,
            )
        }
    return sortMediaListEntries(filtered, sort, order, progressOrder, airingOrder)
}

/**
 * Rail count: ALL counts the whole list, any other section its own entries.
 * Counts reflect the loaded list, never view state (sheet filters, 18+ setting).
 */
internal fun <T> List<T>.sectionCount(isAll: Boolean, predicate: (T) -> Boolean): Int =
    if (isAll) size else count(predicate)
