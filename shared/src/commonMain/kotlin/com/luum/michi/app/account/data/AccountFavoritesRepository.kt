package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.core.network.NetworkResult

internal interface AccountFavoritesRepository {
    /** Fetches the user's favourites: anime, manga, characters, staff, studios. */
    suspend fun loadFavorites(userId: Int): NetworkResult<AccountFavorites>
}
