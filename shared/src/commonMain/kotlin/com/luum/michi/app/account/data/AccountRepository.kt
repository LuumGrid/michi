package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.network.NetworkResult

/** Combined result of a single account data fetch. */
internal data class AccountData(
    val stats: AccountStats,
    val favorites: AccountFavorites,
)

/**
 * Single-query alternative to the two separate [AccountStatsRepository] and
 * [AccountFavoritesRepository] calls. Fetches statistics + favourites in one
 * HTTP round-trip.
 */
internal interface AccountRepository {
    suspend fun loadAccount(userId: Int): NetworkResult<AccountData>
}
