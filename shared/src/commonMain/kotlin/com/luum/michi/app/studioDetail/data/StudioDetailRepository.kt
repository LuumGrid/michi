package com.luum.michi.app.studioDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.studioDetail.presentation.model.StudioDetail
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaPage
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaSort

internal interface StudioDetailRepository {
    suspend fun loadDetail(id: Int, sort: StudioMediaSort): NetworkResult<StudioDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: StudioMediaSort): NetworkResult<StudioMediaPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
