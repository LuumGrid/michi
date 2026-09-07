package com.luum.michi.app.account.domain

import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.core.domain.network.NetworkResult

internal interface AccountFavoritesRepository {
    /** Fetches the user's favourites: anime, manga, characters, staff, studios. */
    suspend fun loadFavorites(userId: Int): NetworkResult<AccountFavorites>
}
