package com.luum.michi.app.core.storage.repository

import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope
import com.luum.michi.app.core.storage.domain.SettingsStore

/** [SortPersistence] over the shared [SettingsStore] (local-only: AniList
 * has no field for sort memory, so this never touches the network). */
internal class StoreSortPersistence(
    private val store: SettingsStore,
) : SortPersistence {
    override fun saveSort(scope: SortScope, sort: String, order: String) {
        store.putString(key(scope, "sort"), sort)
        store.putString(key(scope, "order"), order)
    }

    override fun loadSort(scope: SortScope): Pair<String, String>? {
        val sort = store.getString(key(scope, "sort"))?.takeIf { it.isNotEmpty() } ?: return null
        val order = store.getString(key(scope, "order"))?.takeIf { it.isNotEmpty() } ?: return null
        return sort to order
    }

    override fun clearSort(scope: SortScope) {
        // No remove API on purpose (mirrors SettingsStore surface):
        // blanking reads as absent everywhere loadSort is used.
        store.putString(key(scope, "sort"), "")
        store.putString(key(scope, "order"), "")
    }

    private fun key(scope: SortScope, field: String): String =
        "list_sort_${scope.name.lowercase()}_$field"
}
