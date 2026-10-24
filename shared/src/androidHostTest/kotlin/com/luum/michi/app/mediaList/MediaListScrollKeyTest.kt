package com.luum.michi.app.mediaList

import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.ui.common.mediaListResetKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Scroll reset key: changing what is shown (section, sort, filters) returns the
 * list to the top; the search query is not part of the key, so typing never
 * jumps.
 */
class MediaListScrollKeyTest {

    @Test
    fun theKeyTracksWhatIsShownAndNeverTheQuery() {
        val base = mediaListResetKey(
            selected = AnimeListSection.ALL,
            sortOption = UserListSort.TITLE,
            sortOrder = UserListOrder.ASCENDING,
            season = null,
            genres = emptyList(),
            formats = emptyList(),
            year = null,
        )

        // Same inputs -> same key (the query is not a parameter at all).
        assertEquals(
            base,
            mediaListResetKey(
                AnimeListSection.ALL,
                UserListSort.TITLE,
                UserListOrder.ASCENDING,
                null,
                emptyList(),
                emptyList(),
                null,
            ),
        )
        // Direction or a filter change -> a different key.
        assertNotEquals(
            base,
            mediaListResetKey(
                AnimeListSection.ALL,
                UserListSort.TITLE,
                UserListOrder.DESCENDING,
                null,
                emptyList(),
                emptyList(),
                null,
            ),
        )
        assertNotEquals(
            base,
            mediaListResetKey(
                AnimeListSection.ALL,
                UserListSort.TITLE,
                UserListOrder.ASCENDING,
                null,
                listOf("Action"),
                emptyList(),
                null,
            ),
        )
        // The section is part of the key too.
        assertNotEquals(
            base,
            mediaListResetKey(
                AnimeListSection.WATCHING,
                UserListSort.TITLE,
                UserListOrder.ASCENDING,
                null,
                emptyList(),
                emptyList(),
                null,
            ),
        )
    }
}
