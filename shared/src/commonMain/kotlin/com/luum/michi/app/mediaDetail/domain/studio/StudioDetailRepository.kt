package com.luum.michi.app.mediaDetail.domain.studio

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.model.MediaGridPage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail

internal interface StudioDetailRepository {
    /** [sort] is the AniList `MediaSort` key the grid is ordered by. */
    suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: String): NetworkResult<MediaGridPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
