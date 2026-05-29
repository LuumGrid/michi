package com.luum.michi.app.staffDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.staffDetail.presentation.model.StaffDetail
import com.luum.michi.app.staffDetail.presentation.model.StaffCharacterPage
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaPage
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaSort

internal interface StaffDetailRepository {
    suspend fun loadDetail(id: Int, sort: StaffMediaSort): NetworkResult<StaffDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: StaffMediaSort): NetworkResult<StaffMediaPage>
    suspend fun loadCharacterPage(id: Int, page: Int): NetworkResult<StaffCharacterPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
