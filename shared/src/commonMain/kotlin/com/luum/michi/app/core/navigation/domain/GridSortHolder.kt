package com.luum.michi.app.core.navigation.domain

import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort

/**
 * Server-ordered grid surface (the studio and genre pages): the shared detail
 * sort sheet reads the current choice and asks the holder to re-query the grid
 * from page 1. Pure Kotlin: no Compose and no feature types.
 */
internal interface GridSortHolder {
    val sortOption: UserListSort
    val sortOrder: UserListOrder

    fun changeSort(option: UserListSort, order: UserListOrder)
}
