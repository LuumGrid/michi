package com.luum.michi.app.mediaDetail.domain.studio

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaPage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort

internal interface StudioDetailRepository {
    suspend fun loadDetail(id: Int, sort: StudioMediaSort): NetworkResult<StudioDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: StudioMediaSort): NetworkResult<StudioMediaPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
