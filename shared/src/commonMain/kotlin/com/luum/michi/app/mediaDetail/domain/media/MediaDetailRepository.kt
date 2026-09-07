package com.luum.michi.app.mediaDetail.domain.media

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.model.*

internal interface MediaDetailRepository {
    suspend fun loadDetail(
        mediaId: Int,
        voiceLanguage: String = "JAPANESE",
    ): NetworkResult<MediaDetail>

    suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<MediaCharactersPage>

    suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage>

    suspend fun loadRecommendations(
        mediaId: Int,
    ): NetworkResult<List<MediaRecommendationEntry>>
}
