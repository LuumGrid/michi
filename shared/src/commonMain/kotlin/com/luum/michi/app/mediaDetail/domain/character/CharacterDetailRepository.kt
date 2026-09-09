package com.luum.michi.app.mediaDetail.domain.character

import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaPage
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaSort
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult

internal interface CharacterDetailRepository {
    suspend fun loadDetail(id: Int, sort: CharacterMediaSort, strings: LanguageStrings): NetworkResult<CharacterDetail>
    suspend fun loadMediaPage(id: Int, page: Int, sort: CharacterMediaSort): NetworkResult<CharacterMediaPage>
    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
