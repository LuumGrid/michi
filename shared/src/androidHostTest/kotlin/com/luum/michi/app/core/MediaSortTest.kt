package com.luum.michi.app.core

import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.toMediaSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `UserListSort.toMediaSort` is the single place that turns the shared sort
 * sheet selection into an AniList `MediaSort` key (Explore search + studio grid).
 */
class MediaSortTest {

    @Test
    fun everySortDerivesANonBlankApiKey() {
        UserListSort.entries.forEach { sort ->
            UserListOrder.entries.forEach { order ->
                val key = sort.toMediaSort(order, isAnime = true)
                assertTrue(key.isNotBlank(), "$sort/$order blank")
            }
        }
    }

    @Test
    fun theOrderPicksTheDescendingVariant() {
        assertEquals(
            "POPULARITY_DESC",
            UserListSort.POPULARITY.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "POPULARITY",
            UserListSort.POPULARITY.toMediaSort(UserListOrder.ASCENDING, isAnime = true),
        )
        assertEquals(
            "TITLE_ROMAJI_DESC",
            UserListSort.TITLE.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
    }

    @Test
    fun progressKeyDependsOnTheMediaType() {
        assertEquals(
            "EPISODES_DESC",
            UserListSort.PROGRESS.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "CHAPTERS_DESC",
            UserListSort.PROGRESS.toMediaSort(UserListOrder.DESCENDING, isAnime = false),
        )
        assertEquals(
            "CHAPTERS",
            UserListSort.PROGRESS.toMediaSort(UserListOrder.ASCENDING, isAnime = false),
        )
    }

    @Test
    fun studioCriteriaMapToValidMediaSortKeys() {
        // The studio sheet's options, exactly what the grid sends to AniList.
        assertEquals(
            "POPULARITY_DESC",
            UserListSort.POPULARITY.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "SCORE_DESC",
            UserListSort.AVERAGE_SCORE.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "FAVOURITES_DESC",
            UserListSort.FAVORITES.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "START_DATE_DESC",
            UserListSort.RELEASE_DATE.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
        assertEquals(
            "TITLE_ROMAJI_DESC",
            UserListSort.TITLE.toMediaSort(UserListOrder.DESCENDING, isAnime = true),
        )
    }
}
