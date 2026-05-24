package com.luum.michi.app.animation.data

import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.core.network.NetworkResult

internal interface AnimationListRepository {
    /**
     * Fetches the authenticated user's full anime list, grouped by AniList
     * status and flattened into UI entries.
     */
    suspend fun loadList(userId: Int): NetworkResult<List<AnimationListEntry>>
}
