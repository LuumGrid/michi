package com.luum.michi.app.root

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.root.state.State
import com.luum.michi.app.ui.components.ToolbarAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        isCatalogDetail: Boolean = false,
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
        isCatalogDetail = isCatalogDetail,
        isDetailList = isDetailList,
        isDetailFavourite = isDetailFavourite,
        isGuest = isGuest,
        strings = EnglishStrings,
    )

    private fun ids(actions: List<ToolbarAction>): List<String> = actions.map { it.id }

    private fun tabActions(
        tab: TabSection = TabSection.DISCOVER,
        isExploreResults: Boolean = false,
        isExploreMediaCategory: Boolean = false,
        hasExploreFilters: Boolean = false,
    ): List<ToolbarAction> = toolbarActions(
        tab = tab,
        isSearching = false,
        isSettingsOpen = false,
        isCalendarOpen = false,
        isDetailOpen = false,
        isMediaDetail = false,
        isStudioDetail = false,
        isGenreDetail = false,
        isDetailList = false,
        isDetailFavourite = false,
        isGuest = false,
        isExploreResults = isExploreResults,
        isExploreMediaCategory = isExploreMediaCategory,
        hasExploreFilters = hasExploreFilters,
        strings = EnglishStrings,
    )

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

    /** A dashboard catalog is not an entity either: only the server sort. */
    @Test
    fun catalogDetailShowsOnlyTheSort() {
        assertEquals(listOf("sort"), ids(detailActions(isCatalogDetail = true)))
        assertEquals(listOf("sort"), ids(detailActions(isCatalogDetail = true, isGuest = true)))
    }

    /** The filled twin is used once the entity is favourited (signed-in only). */
    @Test
    fun theHeartSwapsToItsFilledTwinWhenFavourited() {
        val outline = detailActions(isMediaDetail = true).first { it.id == "favourite" }
        val filled = detailActions(isMediaDetail = true, isDetailFavourite = true)
            .first { it.id == "favourite" }

        assertTrue(outline.icon != filled.icon, "the favourited on-state must swap the icon")
    }

    // Explore results mode: the field collapses and the toolbar owns DISCOVER.

    @Test
    fun exploreResultsOnMediaShowFilterSortAndSearchWithoutCalendar() {
        val actions = tabActions(isExploreResults = true, isExploreMediaCategory = true)

        assertEquals(listOf("filter", "sort", "search"), ids(actions))
        assertFalse(actions.any { it.id == "calendar" }, "no calendar in results mode")
        val filter = actions.first { it.id == "filter" }
        val sort = actions.first { it.id == "sort" }
        assertTrue(filter.group != null && filter.group == sort.group, "filter + sort share one capsule")
    }

    @Test
    fun exploreResultsOnPeopleShowOnlySearch() {
        val actions = tabActions(isExploreResults = true, isExploreMediaCategory = false)

        assertEquals(listOf("search"), ids(actions))
    }

    @Test
    fun theFilterBadgeFollowsActiveFilters() {
        assertFalse(
            tabActions(isExploreResults = true, isExploreMediaCategory = true)
                .first { it.id == "filter" }.badge,
        )
        assertTrue(
            tabActions(
                isExploreResults = true,
                isExploreMediaCategory = true,
                hasExploreFilters = true,
            ).first { it.id == "filter" }.badge,
        )
    }

    @Test
    fun exploreResultsNeverLeakIntoAnotherTabsToolbar() {
        val state = State()
        state.enterExploreResults("naruto")
        state.selectTab(TabSection.ANIME)

        // The caller passes the tab-scoped getter, which is false on ANIME, so
        // the list keeps its own actions (calendar + filter + sort + search).
        val actions = tabActions(
            tab = TabSection.ANIME,
            isExploreResults = state.isExploreResultsVisible,
        )

        assertEquals(listOf("calendar", "filter", "sort", "search"), ids(actions))
    }
}
