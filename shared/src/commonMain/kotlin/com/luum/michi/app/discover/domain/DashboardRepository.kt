package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem

/**
 * Items per dashboard rail: 12 fills four columns of the three-row card rail,
 * so the last column never paints a lonely card. Kept separate from the
 * detail/profile `RailPageSize` (10) because only the dashboard stacks cards in
 * threes.
 */
internal const val DashboardRailPageSize = 12

/**
 * Dashboard rails for one media type, keyed by [DashboardSectionKind]. The
 * type-scoped load keeps each request to one type's aliases (≈10), instead of
 * firing every rail in a single heavy query.
 */
internal data class DashboardFeed(
    val sections: Map<DashboardSectionKind, List<MediaItem>> = emptyMap(),
)

internal interface DashboardRepository {
    /**
     * Fetches the [mediaType] dashboard rails in a single GraphQL round trip.
     *
     * [hideAdult] drives the client-side 18+ gate: when true the query pins
     * `isAdult: false`; when false the variable is omitted (null = both).
     * Never sends `isAdult: true` (that would filter to adult ONLY).
     */
    suspend fun loadFeed(
        mediaType: DashboardMediaType,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<DashboardFeed>
}
