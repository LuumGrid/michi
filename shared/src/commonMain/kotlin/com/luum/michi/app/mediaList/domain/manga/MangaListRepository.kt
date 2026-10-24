package com.luum.michi.app.mediaList.domain.manga

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry

internal interface MangaListRepository {
    /**
     * Fetches the authenticated user's full manga list, grouped by AniList
     * status and flattened into UI entries. A successful fetch is persisted so a
     * later cold start can show it before the network.
     */
    suspend fun loadList(userId: Int, splitCompleted: Boolean = true): NetworkResult<List<MangaListEntry>>

    /**
     * The last persisted manga list for [userId], already mapped with
     * [splitCompleted], or null when nothing valid is stored. Never counts as a
     * fresh load: the caller still revalidates against the network.
     */
    suspend fun loadCachedList(userId: Int, splitCompleted: Boolean = true): List<MangaListEntry>?
}
