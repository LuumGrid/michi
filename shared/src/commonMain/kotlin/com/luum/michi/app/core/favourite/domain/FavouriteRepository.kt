package com.luum.michi.app.core.favourite.domain

import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.flow.StateFlow

/**
 * What a favourite toggle can target. The single [FavouriteRepository] mutation
 * carries all five as optional variables, so one call handles every case.
 */
internal sealed interface FavouriteTarget {
    val id: Int

    data class Anime(override val id: Int) : FavouriteTarget
    data class Manga(override val id: Int) : FavouriteTarget
    data class Character(override val id: Int) : FavouriteTarget
    data class Staff(override val id: Int) : FavouriteTarget
    data class Studio(override val id: Int) : FavouriteTarget
}

/**
 * One favourite toggle for every entity type. The only consumer today is
 * mediaDetail — directly for character/staff/studio and through
 * `MediaListEntryRepository` for anime/manga — and profile will use it to
 * unfavourite. One mutation instead of a copy per feature.
 */
internal interface FavouriteRepository {
    /**
     * Monotonic revision, bumped after every successful toggle. Holders that
     * cache favourite state (the profile, the favourites grid) store the revision
     * they loaded at and compare it on open: a change means their mirror is stale
     * and must be reloaded. It adds no requests by itself.
     */
    val revision: StateFlow<Int>

    suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit>
}
