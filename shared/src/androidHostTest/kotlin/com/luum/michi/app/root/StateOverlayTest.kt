package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The overlay set and its progressive back order (see [State.nextBackStep]). */
class StateOverlayTest {

    @Test
    fun backClosesEverySheetAtOnceBeforeSettings() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.SectionFilter)
        state.open(RootOverlay.ListSort)

        assertEquals(BackStep.CloseSheet, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.SectionFilter))
        assertFalse(state.isOpen(RootOverlay.ListSort))
        assertTrue(state.isOpen(RootOverlay.Settings), "every sheet closes before Settings")

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Settings))
    }

    @Test
    fun backClosesSettingsAndSignInTogether() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.SignIn)

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.Settings))
        assertFalse(state.isOpen(RootOverlay.SignIn))
    }

    @Test
    fun detailClosesBeforeTheOverlayBelowIt() {
        val state = State()
        state.openMedia(1)
        state.open(RootOverlay.Calendar)

        assertEquals(BackStep.CloseDetail, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isDetailOpen)
        assertTrue(state.isOpen(RootOverlay.Calendar), "the detail closes before the overlay below")

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Calendar))
    }

    @Test
    fun detailOpenedFromSearchClosesBeforeTheSearch() {
        val state = State()
        state.openSearch(TabSection.DISCOVER)
        state.updateSearch(TabSection.DISCOVER, "naruto")
        state.openMedia(1)

        // The first back closes the detail; the search behind it survives.
        assertEquals(BackStep.CloseDetail, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isDetailOpen)
        assertTrue(state.isSearchActive, "the search stays active under the detail")

        // The second back (now the topmost layer) closes the search.
        assertEquals(BackStep.CloseSearch, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isSearchActive)
    }

    @Test
    fun aSheetOverSearchClosesBeforeTheSearch() {
        val state = State()
        state.openSearch(TabSection.DISCOVER)
        state.open(RootOverlay.ListSort)

        assertEquals(BackStep.CloseSheet, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.ListSort))
        assertTrue(state.isSearchActive, "the sheet closes before the search")
    }

    @Test
    fun searchIsHiddenWhileASurfaceDrawsOverIt() {
        val state = State()
        state.openSearch(TabSection.DISCOVER)
        state.updateSearch(TabSection.DISCOVER, "naruto")
        assertTrue(state.isSearchVisible)

        // A detail opened from the results replaces the search field...
        state.openMedia(1)
        assertFalse(state.isSearchVisible, "the detail owns the toolbar")
        assertTrue(state.isSearchActive, "but the search itself stays open")

        // ...and back brings it back, query intact.
        assertTrue(state.applyBackStep())
        assertTrue(state.isSearchVisible)
        assertEquals("naruto", state.searchQuery(TabSection.DISCOVER))

        // An above-detail overlay (Settings) also covers it.
        state.open(RootOverlay.Settings)
        assertFalse(state.isSearchVisible)
    }

    @Test
    fun exploreResultsCloseOnBackAfterTheFieldAndDetail() {
        val state = State()
        state.enterExploreResults("naruto")

        // Results alone: back leaves the mode and clears the sent query.
        assertTrue(state.isExploreResultsVisible)
        assertEquals(BackStep.CloseExploreResults, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.exploreResults)
        assertEquals("", state.searchQuery(TabSection.DISCOVER))
    }

    @Test
    fun aDetailOpenedFromExploreResultsClosesBeforeTheResults() {
        val state = State()
        state.enterExploreResults("naruto")
        state.openMedia(1)

        assertEquals(BackStep.CloseDetail, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isDetailOpen)
        assertTrue(state.exploreResults, "the results mode survives the detail")
        assertEquals(BackStep.CloseExploreResults, state.nextBackStep())
    }

    @Test
    fun aReopenedFieldClosesBeforeTheExploreResults() {
        val state = State()
        state.enterExploreResults("naruto")
        state.openSearch(TabSection.DISCOVER)

        assertEquals(BackStep.CloseSearch, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isSearchActive)
        assertTrue(state.isExploreResultsVisible, "closing the field keeps the results mode")
        assertEquals(BackStep.CloseExploreResults, state.nextBackStep())
    }

    @Test
    fun exploreResultsSurviveATabChangeButStayHidden() {
        val state = State()
        state.enterExploreResults("naruto")
        state.selectTab(TabSection.ANIME)

        assertTrue(state.exploreResults, "the mode survives a tab change")
        assertFalse(state.isExploreResultsVisible, "…but never owns ANIME's toolbar")
        assertEquals(BackStep.SystemDefault, state.nextBackStep())

        state.selectTab(TabSection.DISCOVER)
        assertTrue(state.isExploreResultsVisible)
        assertEquals(BackStep.CloseExploreResults, state.nextBackStep())
    }

    @Test
    fun closingTheFieldRestoresTheSentQuery() {
        val state = State()
        state.enterExploreResults("naruto")
        state.openSearch(TabSection.DISCOVER)
        state.updateSearch(TabSection.DISCOVER, "naruto x")

        state.closeSearch()

        assertEquals("naruto", state.searchQuery(TabSection.DISCOVER))
    }

    @Test
    fun backFromTheCatalogReturnsToTheDashboard() {
        val state = State()
        state.openCatalog("POPULAR_ANIME")

        assertEquals(DetailDestination.Catalog("POPULAR_ANIME"), state.currentDetail)
        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail, "the catalog pops back to the Dashboard")
    }

    @Test
    fun openingADetailFromTheCatalogKeepsIt() {
        val state = State()
        state.openCatalog("POPULAR_ANIME")
        state.openMedia(5)

        assertTrue(state.applyBackStep())
        assertEquals(
            DetailDestination.Catalog("POPULAR_ANIME"),
            state.currentDetail,
            "back from the media leaves the catalog",
        )
    }

    @Test
    fun resetClosesEveryOverlay() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.Calendar)
        state.open(RootOverlay.SectionFilter)
        state.openMedia(1)

        state.reset()

        assertTrue(state.openOverlays.isEmpty())
        assertFalse(state.isDetailOpen)
    }

    @Test
    fun closingAnOverlayThatIsNotOpenChangesNothing() {
        val state = State()
        state.open(RootOverlay.Settings)
        val before = state.openOverlays

        state.close(RootOverlay.Calendar)

        assertEquals(before, state.openOverlays)
        assertTrue(state.isOpen(RootOverlay.Settings))
        assertFalse(state.isOpen(RootOverlay.Calendar))
    }
}
