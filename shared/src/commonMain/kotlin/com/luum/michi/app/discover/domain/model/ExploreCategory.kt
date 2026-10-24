package com.luum.michi.app.discover.domain.model

internal enum class ExploreCategory {
    ANIME,
    MANGA,
    CHARACTERS,
    STAFF,
    STUDIOS
}

/** The detail surface a search result of a given category opens. */
internal enum class ExploreResultTarget { MEDIA, CHARACTER, STAFF, STUDIO }

/**
 * Pure mapping from the active Explore category to the detail a tapped result
 * opens. Kept out of the composable so it is trivially testable.
 */
internal fun ExploreCategory.resultTarget(): ExploreResultTarget = when (this) {
    ExploreCategory.ANIME,
    ExploreCategory.MANGA -> ExploreResultTarget.MEDIA
    ExploreCategory.CHARACTERS -> ExploreResultTarget.CHARACTER
    ExploreCategory.STAFF -> ExploreResultTarget.STAFF
    ExploreCategory.STUDIOS -> ExploreResultTarget.STUDIO
}
