package com.luum.michi.app.core.session

import com.luum.michi.app.core.network.NetworkResult

/**
 * Fetches the currently-authenticated AniList viewer. Implementations talk to
 * AniList via `AniListGraphQLClient`. Wired up in Iteration 2.
 */
internal interface AniListViewerRepository {
    suspend fun fetchViewer(): NetworkResult<Viewer>
}
