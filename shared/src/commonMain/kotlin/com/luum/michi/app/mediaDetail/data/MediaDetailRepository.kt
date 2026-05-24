package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffPage

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
}
