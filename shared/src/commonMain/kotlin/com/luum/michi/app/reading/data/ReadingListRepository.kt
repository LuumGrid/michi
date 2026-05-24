package com.luum.michi.app.reading.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.reading.presentation.model.ReadingListEntry

internal interface ReadingListRepository {
    /**
     * Fetches the authenticated user's full manga list, grouped by AniList
     * status and flattened into UI entries.
     */
    suspend fun loadList(userId: Int): NetworkResult<List<ReadingListEntry>>
}
