package com.luum.michi.app.discover.domain.model

/**
 * The applied Explore filters. The filter sheet edits its own local draft of
 * this value and commits it once on close (never per tap, to respect AniList's
 * request budget); [ExploreFilters.Empty] is the "no filters" state a fresh
 * search starts from.
 */
internal data class ExploreFilters(
    val season: String? = null,
    val genres: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val year: Int? = null,
    /** null = any, false = not in the user's list, true = in the list. */
    val onList: Boolean? = null,
) {
    companion object {
        val Empty = ExploreFilters()
    }
}
