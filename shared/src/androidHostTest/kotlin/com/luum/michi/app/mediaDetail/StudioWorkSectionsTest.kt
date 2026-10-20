package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.mediaDetail.domain.studio.model.NoYearSectionKey
import com.luum.michi.app.mediaDetail.domain.studio.model.studioWorkSections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudioWorkSectionsTest {

    private fun item(id: Int, year: Int?) = MediaGridItem(
        mediaId = id,
        title = "Media $id",
        cover = null,
        paletteHex = null,
        format = "TV",
        year = year,
        averageScore = null,
        favourites = null,
    )

    @Test
    fun releaseDateDescendingGroupsConsecutiveYears() {
        val sections = listOf(item(1, 2026), item(2, 2026), item(3, 2025), item(4, 2024))
            .studioWorkSections(UserListSort.RELEASE_DATE, "Sin fecha")

        assertEquals(listOf("2026", "2025", "2024"), sections?.map { it.title })
        assertEquals(listOf(2, 1, 1), sections?.map { it.items.size })
    }

    @Test
    fun releaseDateAscendingGroupsConsecutiveYears() {
        val sections = listOf(item(1, 2024), item(2, 2025), item(3, 2026))
            .studioWorkSections(UserListSort.RELEASE_DATE, "Sin fecha")

        assertEquals(listOf("2024", "2025", "2026"), sections?.map { it.title })
    }

    @Test
    fun anyOtherSortKeepsTheGridFlat() {
        val items = listOf(item(1, 2026), item(2, 2026), item(3, 2025))

        assertNull(items.studioWorkSections(UserListSort.POPULARITY, "Sin fecha"))
        assertNull(items.studioWorkSections(UserListSort.TITLE, "Sin fecha"))
        assertNull(items.studioWorkSections(UserListSort.FAVORITES, "Sin fecha"))
    }

    @Test
    fun itemsWithoutAYearFormTheirOwnGroupWhereTheServerPutThem() {
        val sections = listOf(item(1, 2025), item(2, null), item(3, null), item(4, 2024))
            .studioWorkSections(UserListSort.RELEASE_DATE, "Sin fecha")

        assertEquals(listOf("2025", "Sin fecha", "2024"), sections?.map { it.title })
        assertEquals(listOf(1, 2, 1), sections?.map { it.items.size })
        assertEquals(NoYearSectionKey, sections?.get(1)?.key)
    }

    @Test
    fun aSecondPageStartingWithTheLastYearJoinsThatSection() {
        val firstPage = listOf(item(1, 2025), item(2, 2025))
        val secondPage = listOf(item(3, 2025), item(4, 2024))

        val sections = (firstPage + secondPage)
            .studioWorkSections(UserListSort.RELEASE_DATE, "Sin fecha")

        assertEquals(listOf("2025", "2024"), sections?.map { it.title })
        assertEquals(3, sections?.first()?.items?.size)
    }

    @Test
    fun anEmptyListHasNoSections() {
        assertEquals(
            emptyList(),
            emptyList<MediaGridItem>().studioWorkSections(UserListSort.RELEASE_DATE, "Sin fecha"),
        )
    }
}
