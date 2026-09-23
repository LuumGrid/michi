package com.luum.michi.app.core.medialist.domain

import com.luum.michi.app.core.network.domain.NetworkResult

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
    ): NetworkResult<MediaListViewerEntry>

    /**
     * Partial progress save: null fields are omitted from the mutation, so
     * the server leaves them intact. Novels ([tracksByVolume]-style entries)
     * send volumes only, with [progress] null.
     */
    suspend fun saveProgress(
        mediaId: Int,
        progress: Int?,
        status: MediaListStatus? = null,
        progressVolumes: Int? = null,
    ): NetworkResult<Unit>

    suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit>

    suspend fun deleteEntry(entryId: Int): NetworkResult<Unit>
}
