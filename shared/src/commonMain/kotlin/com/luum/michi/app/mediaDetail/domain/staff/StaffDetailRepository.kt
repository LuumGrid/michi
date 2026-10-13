package com.luum.michi.app.mediaDetail.domain.staff

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterPage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaPage

internal interface StaffDetailRepository {
    suspend fun loadDetail(id: Int, strings: LanguageStrings): NetworkResult<StaffDetail>
    suspend fun loadMediaPage(id: Int, page: Int): NetworkResult<StaffMediaPage>
    suspend fun loadCharacterPage(id: Int, page: Int): NetworkResult<StaffCharacterPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
