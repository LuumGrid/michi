package com.luum.michi.app.mediaList.domain.common

import com.luum.michi.app.core.domain.model.MediaFormat
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.model.UserListOrder
import com.luum.michi.app.core.domain.model.UserListSort

/**
 * Sort/filter fields shared verbatim by the anime and manga list entries.
 * Anything genuinely different (progress shape, status enums, next-release
 * data) stays out on purpose — see sortMediaListEntries.
 */
internal interface SortableMediaListEntry {
    val id: Int
    val title: String
    val score: Double
    val updatedAt: Long
    val startedAtInt: Int
    val completedAtInt: Int
    val releaseDateInt: Int
    val averageScore: Int
    val popularity: Int
    val favouritesCount: Int
    val trending: Int
    val priority: Int
    val originalIndex: Int
    val season: MediaSeason?
    val genres: List<String>
    val seasonYear: Int?
    val format: MediaFormat
}

private val commonComparators: Map<UserListSort, Comparator<SortableMediaListEntry>> = mapOf(
    UserListSort.FOLLOW_LIST to compareBy { it.originalIndex },
    UserListSort.TITLE to compareBy { it.title },
    UserListSort.SCORE to compareBy { it.score },
    UserListSort.LAST_UPDATED to compareBy { it.updatedAt },
    UserListSort.LAST_ADDED to compareBy { it.id },
    UserListSort.START_DATE to compareBy { it.startedAtInt },
    UserListSort.COMPLETED_DATE to compareBy { it.completedAtInt },
    UserListSort.RELEASE_DATE to compareBy { it.releaseDateInt },
    UserListSort.AVERAGE_SCORE to compareBy { it.averageScore },
    UserListSort.POPULARITY to compareBy { it.popularity },
    UserListSort.FAVORITES to compareBy { it.favouritesCount },
    UserListSort.TRENDING to compareBy { it.trending },
    UserListSort.PRIORITY to compareBy { it.priority },
)

/**
 * Shared sort for both user lists. The 13 common keys live in
 * commonComparators exactly once; PROGRESS and NEXT_AIRING genuinely differ
 * per list (episodes vs chapters, airing date vs list order) and arrive as
 * comparators built at the call site with fully inferred types.
 *
 * Mirrors the previous per-holder logic 1:1, including `reversed()` for
 * descending — deliberately not `sortedByDescending`, which orders ties
 * differently.
 */
internal fun <T : SortableMediaListEntry> sortMediaListEntries(
    entries: List<T>,
    sort: UserListSort,
    order: UserListOrder,
    progressOrder: Comparator<T>,
    airingOrder: Comparator<T>,
): List<T> {
    val comparator: Comparator<in T> = when (sort) {
        UserListSort.PROGRESS -> progressOrder
        UserListSort.NEXT_AIRING -> airingOrder
        else -> commonComparators.getValue(sort)
    }
    val sorted = entries.sortedWith(comparator)
    return if (order == UserListOrder.DESCENDING) sorted.reversed() else sorted
}
