package com.luum.michi.app.core.storage.domain

/** Sort memory slot: one per list tab (anime/manga never share restores). */
internal enum class SortScope { ANIME, MANGA }

internal interface SortPersistence {
    fun saveSort(scope: SortScope, sort: String, order: String)
    fun loadSort(scope: SortScope): Pair<String, String>?
    fun clearSort(scope: SortScope)
}
