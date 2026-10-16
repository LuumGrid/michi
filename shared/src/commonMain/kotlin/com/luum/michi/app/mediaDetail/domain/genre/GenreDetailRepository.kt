package com.luum.michi.app.mediaDetail.domain.genre

import com.luum.michi.app.core.model.MediaType
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.model.MediaGridPage

/**
 * Works of one genre. A genre is not an AniList entity (there is nothing to
 * fetch beyond the page), so this is a single paged query: page 1 is the first
 * load and the same call pages further. [sort] is the AniList `MediaSort` key
 * the grid is ordered by; [hideAdult] pins `isAdult: false` when the viewer has
 * 18+ content hidden (omitted otherwise, which means both).
 */
internal interface GenreDetailRepository {
    suspend fun loadPage(
        name: String,
        type: MediaType,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<MediaGridPage>
}
