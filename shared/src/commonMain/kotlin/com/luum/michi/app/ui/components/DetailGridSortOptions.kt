package com.luum.michi.app.ui.components

import com.luum.michi.app.core.model.UserListSort

/**
 * Sorts the detail poster grids offer (studio works, genre works). Each one maps
 * to a valid AniList `MediaSort` through `UserListSort.toMediaSort`; the grid is
 * ordered by the server, so the other criteria of the shared sheet would not
 * change anything here.
 */
internal val DetailGridSortOptions = listOf(
    UserListSort.POPULARITY,
    UserListSort.AVERAGE_SCORE,
    UserListSort.FAVORITES,
    UserListSort.RELEASE_DATE,
    UserListSort.TITLE,
)
