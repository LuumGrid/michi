package com.luum.michi.app.mediaList.domain.manga

import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry

internal interface MangaListRepository {
    /**
     * Fetches the authenticated user's full manga list, grouped by AniList
     * status and flattened into UI entries.
     */
    suspend fun loadList(userId: Int): NetworkResult<List<MangaListEntry>>
}
