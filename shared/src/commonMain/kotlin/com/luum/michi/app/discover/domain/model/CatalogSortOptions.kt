package com.luum.michi.app.discover.domain.model

import com.luum.michi.app.core.medialist.domain.UserListSort

/**
 * Sort criteria a catalog surface offers (Explore's results and the Dashboard's
 * "View all" catalog pages). Every one maps to a valid AniList `MediaSort`
 * through `UserListSort.toMediaSort`, so the list is server-ordered and no
 * client re-sort is needed. List-only criteria (progress, last updated, …) have
 * no place in a catalog search. The genre/studio grids keep the narrower
 * `DetailGridSortOptions`.
 */
internal val CatalogSortOptions = listOf(
    UserListSort.TRENDING,
    UserListSort.POPULARITY,
    UserListSort.AVERAGE_SCORE,
    UserListSort.FAVORITES,
    UserListSort.RELEASE_DATE,
    UserListSort.TITLE,
)
