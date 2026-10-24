package com.luum.michi.app.core.medialist.domain

/** Sort memory restore: unknown/corrupt names fall back to null (holder
 * defaults), never crash. Pure for tests. */
internal fun hydrateSort(
    persistence: SortPersistence,
    scope: SortScope,
): Pair<UserListSort, UserListOrder>? {
    val (sort, order) = persistence.loadSort(scope) ?: return null
    val option = UserListSort.entries.firstOrNull { it.name == sort } ?: return null
    val direction = UserListOrder.entries.firstOrNull { it.name == order } ?: return null
    return option to direction
}

/** Session-start sort per tab. Pure for tests. */
internal data class InitialSort(
    val option: UserListSort,
    val order: UserListOrder,
    /** True only when restoring the user's own saved sort. */
    val persist: Boolean,
)

/**
 * Precedence: saved persist-sort wins, then the caller-supplied default sort,
 * then holder defaults (handled by the caller when this returns the default —
 * persist is false so defaults are never written back as user choices).
 * Explicit always beats default: changing Default sort while a saved sort
 * exists leaves the lists on the saved sort.
 */
internal fun resolveInitialSort(
    saved: Pair<UserListSort, UserListOrder>?,
    defaultSort: Pair<UserListSort, UserListOrder>,
): InitialSort {
    if (saved != null) {
        return InitialSort(saved.first, saved.second, persist = true)
    }
    return InitialSort(defaultSort.first, defaultSort.second, persist = false)
}
