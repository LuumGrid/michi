package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailViewerEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus

internal interface MediaListEntryRepository {
    suspend fun saveEntry(
        mediaId: Int,
        status: MediaListStatus,
        progress: Int,
        progressVolumes: Int?,
        score: Float,
        notes: String,
        repeat: Int,
        priority: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
        startedAtMillis: Long?,
        completedAtMillis: Long?,
    ): NetworkResult<MediaDetailViewerEntry>

    suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit>
}
