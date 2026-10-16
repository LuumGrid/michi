package com.luum.michi.app.mediaList.ui.common.state

import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.network.domain.NetworkError

/**
 * Read/update surface the shared list screen ([com.luum.michi.app.mediaList.ui
 * .common.MediaListScreenScaffold]) needs from a list holder, so it can stay
 * generic instead of taking a lambda per field. The per-type `+1` increments
 * are deliberately out: they go through the card slot.
 */
internal interface MediaListHolder<T, S> {
    val entries: List<T>
    val isLoading: Boolean
    val error: NetworkError?
    val actionError: NetworkError?
    val currentSortOption: UserListSort
    val currentSortOrder: UserListOrder
    val filterSeason: MediaSeason?
    val filterGenres: List<String>
    val filterFormats: List<String>
    val filterYear: Int?

    fun entriesInSection(section: S, hideAdult: Boolean = false): List<T>
    fun countInSection(section: S): Int
    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean)
    fun updateListFilters(season: MediaSeason?, genres: List<String>, formats: List<String>, year: Int?)
    fun clearActionError()
}
