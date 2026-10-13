package com.luum.michi.app.mediaDetail.domain.media

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.model.*

internal interface MediaDetailRepository {
    suspend fun loadDetail(
        mediaId: Int,
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail>

    suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaCharactersPage>

    suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage>

    suspend fun loadRecommendations(
        mediaId: Int,
        page: Int = 1,
    ): NetworkResult<MediaRecommendationsPage>
}
