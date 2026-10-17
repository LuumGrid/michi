package com.luum.michi.app.root

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.ui.components.ToolbarAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Detail trailing actions per scope. A guest has no list entry and no session to
 * favourite with, so the pencil and heart must never render (their ids can
 * still arrive from a stale toolbar; the action handlers are the last guard).
 */
class ToolbarActionsTest {

    private fun detailActions(
        isMediaDetail: Boolean = false,
        isStudioDetail: Boolean = false,
        isGenreDetail: Boolean = false,
        isDetailList: Boolean = false,
        isDetailFavourite: Boolean = false,
        isGuest: Boolean = false,
    ): List<ToolbarAction> = toolbarActions(
        tab = TabSection.DISCOVER,
        isSearching = false,
        isSettingsOpen = false,
        isCalendarOpen = false,
        isDetailOpen = true,
        isMediaDetail = isMediaDetail,
        isStudioDetail = isStudioDetail,
        isGenreDetail = isGenreDetail,
        isDetailList = isDetailList,
        isDetailFavourite = isDetailFavourite,
        isGuest = isGuest,
        strings = EnglishStrings,
    )

    private fun ids(actions: List<ToolbarAction>): List<String> = actions.map { it.id }

    @Test
    fun guestMediaShowsOnlyTheOverflow() {
        assertEquals(listOf("more"), ids(detailActions(isMediaDetail = true, isGuest = true)))
    }

    @Test
    fun guestPersonShowsOnlyTheOverflow() {
        // Character and staff share the same shape (no media list entry).
        assertEquals(listOf("more"), ids(detailActions(isGuest = true)))
    }

    @Test
    fun guestStudioAndGenreKeepTheirSort() {
        assertEquals(listOf("sort", "more"), ids(detailActions(isStudioDetail = true, isGuest = true)))
        assertEquals(listOf("sort", "more"), ids(detailActions(isGenreDetail = true, isGuest = true)))
    }

    @Test
    fun guestViewAllListHasNoActions() {
        assertTrue(detailActions(isDetailList = true, isGuest = true).isEmpty())
    }

    @Test
    fun signedInMediaKeepsEditAndFavourite() {
        assertEquals(
            listOf("edit", "favourite", "more"),
            ids(detailActions(isMediaDetail = true)),
        )
    }

    @Test
    fun signedInPersonKeepsTheFavourite() {
        assertEquals(listOf("favourite", "more"), ids(detailActions()))
    }

    @Test
    fun signedInStudioAndGenreKeepFavouriteWhereTheyHaveOne() {
        assertEquals(
            listOf("sort", "favourite", "more"),
            ids(detailActions(isStudioDetail = true)),
        )
        // A genre is not an entity: sort + overflow only.
        assertEquals(listOf("sort", "more"), ids(detailActions(isGenreDetail = true)))
    }

    /** The filled twin is used once the entity is favourited (signed-in only). */
    @Test
    fun theHeartSwapsToItsFilledTwinWhenFavourited() {
        val outline = detailActions(isMediaDetail = true).first { it.id == "favourite" }
        val filled = detailActions(isMediaDetail = true, isDetailFavourite = true)
            .first { it.id == "favourite" }

        assertTrue(outline.icon != filled.icon, "the favourited on-state must swap the icon")
    }
}
