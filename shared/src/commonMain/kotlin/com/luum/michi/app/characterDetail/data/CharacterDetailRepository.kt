package com.luum.michi.app.characterDetail.data

import com.luum.michi.app.characterDetail.presentation.model.CharacterDetail
import com.luum.michi.app.characterDetail.presentation.model.CharacterMediaPage
import com.luum.michi.app.characterDetail.presentation.model.CharacterMediaSort
import com.luum.michi.app.core.network.NetworkResult

internal interface CharacterDetailRepository {
    suspend fun loadDetail(id: Int, sort: CharacterMediaSort): NetworkResult<CharacterDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: CharacterMediaSort): NetworkResult<CharacterMediaPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
