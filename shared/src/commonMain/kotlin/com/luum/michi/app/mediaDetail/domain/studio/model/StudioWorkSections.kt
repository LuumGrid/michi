package com.luum.michi.app.mediaDetail.domain.studio.model

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.medialist.domain.UserListSort

/** One year (or the no-year bucket) of a studio's works. */
internal data class StudioWorkSection(
    val key: String,
    val title: String,
    val items: List<MediaGridItem>,
)

internal const val NoYearSectionKey = "no-year"

/**
 * Groups a studio's works by year for the release-date order. The server already
 * ordered the list; only consecutive items with the same year are grouped, so
 * nothing is reordered. Items without a year form their own group at the
 * position the server gave them, titled [noYearLabel].
 *
 * Returns null for every other sort: the grid stays flat.
 */
internal fun List<MediaGridItem>.studioWorkSections(
    sort: UserListSort,
    noYearLabel: String,
): List<StudioWorkSection>? {
    if (sort != UserListSort.RELEASE_DATE) return null
    if (isEmpty()) return emptyList()

    val sections = mutableListOf<StudioWorkSection>()
    var currentYear: Int? = null
    var currentItems = mutableListOf<MediaGridItem>()
    var hasCurrent = false

    fun flush() {
        if (!hasCurrent) return
        sections += StudioWorkSection(
            key = currentYear?.toString() ?: NoYearSectionKey,
            title = currentYear?.toString() ?: noYearLabel,
            items = currentItems.toList(),
        )
    }

    for (item in this) {
        if (!hasCurrent || item.year != currentYear) {
            flush()
            currentYear = item.year
            currentItems = mutableListOf()
            hasCurrent = true
        }
        currentItems += item
    }
    flush()
    return sections
}
