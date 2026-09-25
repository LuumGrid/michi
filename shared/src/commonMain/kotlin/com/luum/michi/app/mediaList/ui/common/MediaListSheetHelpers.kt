package com.luum.michi.app.mediaList.ui.common

import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry

/** Option id for the "any" season row (matches mediaListSeasonOptions). */
internal const val ANY_SEASON_ID = "any"

internal fun List<String>.toggled(value: String): List<String> =
    if (value in this) this - value else this + value

/**
 * Year filter tabs for the shared sheet: "any" (whole loaded list) plus
 * one chip per year, each with its entry count — same language as the
 * section rails. Counts run over the whole loaded list since filters
 * apply cross-section.
 */
internal fun yearFilterTabs(
    entries: List<SortableMediaListEntry>,
    years: List<Int?>,
    anyLabel: String,
): List<MediaListSectionTab<Int?>> {
    val counts = entries.groupingBy { it.seasonYear }.eachCount()
    return years.map { year ->
        MediaListSectionTab(
            value = year,
            label = year?.toString() ?: anyLabel,
            count = if (year == null) entries.size else counts[year] ?: 0,
        )
    }
}
