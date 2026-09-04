package com.luum.michi.app.manga.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.manga.presentation.model.MangaListEntry

internal interface MangaListRepository {
    /**
     * Fetches the authenticated user's full manga list, grouped by AniList
     * status and flattened into UI entries.
     */
    suspend fun loadList(userId: Int): NetworkResult<List<MangaListEntry>>
}
