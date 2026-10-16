package com.luum.michi.app.mediaDetail.domain.studio

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.model.MediaGridItem
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail

internal interface StudioDetailRepository {
    /** [sort] is the AniList `MediaSort` key the grid is ordered by. */
    suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: String): NetworkResult<Page<MediaGridItem>>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
