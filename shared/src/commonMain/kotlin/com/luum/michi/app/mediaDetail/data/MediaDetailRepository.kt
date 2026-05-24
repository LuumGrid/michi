package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail

internal interface MediaDetailRepository {
    suspend fun loadDetail(mediaId: Int): NetworkResult<MediaDetail>
}
