package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.*

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

    suspend fun loadReviewsPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaReviewsPage>

    suspend fun loadThreadsPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaThreadsPage>

    suspend fun loadFollowingEntries(
        mediaId: Int,
    ): NetworkResult<List<MediaFollowingEntry>>

    suspend fun loadActivitiesPage(
        mediaId: Int,
        page: Int,
        userId: Int? = null,
        isFollowing: Boolean? = null,
    ): NetworkResult<MediaActivitiesPage>

    suspend fun loadRecommendations(
        mediaId: Int,
    ): NetworkResult<List<MediaRecommendationEntry>>
}
