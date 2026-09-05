package com.luum.michi.app.anime.domain

import com.luum.michi.app.anime.domain.model.AnimeListEntry
import com.luum.michi.app.core.network.NetworkResult

internal interface AnimeListRepository {
    /**
     * Fetches the authenticated user's full anime list, grouped by AniList
     * status and flattened into UI entries.
     */
    suspend fun loadList(userId: Int): NetworkResult<List<AnimeListEntry>>
}
