package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.network.NetworkResult

internal interface AccountStatsRepository {
    /** Fetches anime/manga counts + follower/following counts for a user. */
    suspend fun loadStats(userId: Int): NetworkResult<AccountStats>
}
