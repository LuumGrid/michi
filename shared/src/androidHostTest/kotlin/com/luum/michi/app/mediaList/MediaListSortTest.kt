package com.luum.michi.app.mediaList

import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import com.luum.michi.app.mediaList.domain.common.sortMediaListEntries
import kotlin.test.Test
import kotlin.test.assertEquals

private data class FakeEntry(
    override val id: Int,
    override val title: String = "Title $id",
    override val score: Double = 0.0,
    override val updatedAt: Long = 0L,
    override val startedAtInt: Int = 0,
    override val completedAtInt: Int = 0,
    override val releaseDateInt: Int = 0,
    override val averageScore: Int = 0,
    override val popularity: Int = 0,
    override val favouritesCount: Int = 0,
    override val trending: Int = 0,
    override val priority: Int = 0,
    override val originalIndex: Int = id,
    override val season: MediaSeason? = null,
    override val genres: List<String> = emptyList(),
    override val seasonYear: Int? = null,
    override val format: MediaFormat = MediaFormat.TV,
) : SortableMediaListEntry

private fun sort(
    entries: List<FakeEntry>,
    sort: UserListSort,
    order: UserListOrder = UserListOrder.ASCENDING,
    progressOrder: Comparator<FakeEntry> = compareBy { it.id },
    airingOrder: Comparator<FakeEntry> = compareBy { it.id },
): List<Int> = sortMediaListEntries(entries, sort, order, progressOrder, airingOrder).map { it.id }

class MediaListSortTest {

    @Test
    fun titleSortsAlphabeticallyAscending() {
        val entries = listOf(FakeEntry(1, title = "Naruto"), FakeEntry(2, title = "Bleach"))
        assertEquals(listOf(2, 1), sort(entries, UserListSort.TITLE))
    }

    @Test
    fun scoreDescendingReversesAscending() {
        val entries = listOf(FakeEntry(1, score = 7.0), FakeEntry(2, score = 9.0))
        assertEquals(listOf(1, 2), sort(entries, UserListSort.SCORE))
        assertEquals(listOf(2, 1), sort(entries, UserListSort.SCORE, UserListOrder.DESCENDING))
    }

    @Test
    fun tiesFlipUnderDescendingProvingReversedSemantics() {
        val entries = listOf(FakeEntry(1, score = 8.0), FakeEntry(2, score = 8.0))
        assertEquals(listOf(1, 2), sort(entries, UserListSort.SCORE))
        // sortedByDescending would keep [1, 2]; reversed() flips the tie.
        assertEquals(listOf(2, 1), sort(entries, UserListSort.SCORE, UserListOrder.DESCENDING))
    }

    @Test
    fun followListUsesOriginalIndexNotId() {
        val entries = listOf(
            FakeEntry(1, title = "B").copy(originalIndex = 1),
            FakeEntry(2, title = "A").copy(originalIndex = 0),
        )
        assertEquals(listOf(2, 1), sort(entries, UserListSort.FOLLOW_LIST))
    }

    @Test
    fun progressUsesInjectedComparator() {
        val entries = listOf(FakeEntry(1), FakeEntry(2), FakeEntry(3))
        val byEvenFirst = compareBy<FakeEntry> { it.id % 2 }
        assertEquals(listOf(2, 1, 3), sort(entries, UserListSort.PROGRESS, progressOrder = byEvenFirst))
    }

    @Test
    fun airingUsesInjectedComparator() {
        val entries = listOf(FakeEntry(1), FakeEntry(2), FakeEntry(3))
        val byIdDesc = compareByDescending<FakeEntry> { it.id }
        assertEquals(listOf(3, 2, 1), sort(entries, UserListSort.NEXT_AIRING, airingOrder = byIdDesc))
    }
}
