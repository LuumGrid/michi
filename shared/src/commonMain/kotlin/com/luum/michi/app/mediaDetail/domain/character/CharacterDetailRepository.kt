package com.luum.michi.app.mediaDetail.domain.character

import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult

internal interface CharacterDetailRepository {
    suspend fun loadDetail(
        id: Int,
        voiceLanguage: String,
        strings: LanguageStrings,
    ): NetworkResult<CharacterDetail>

    suspend fun loadMediaPage(
        id: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<Page<CharacterMediaItem>>

    suspend fun toggleFavourite(id: Int): NetworkResult<Unit>
}
