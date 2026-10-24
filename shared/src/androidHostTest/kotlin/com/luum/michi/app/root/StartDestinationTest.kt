package com.luum.michi.app.root

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.root.state.resolveStartDestination
import com.luum.michi.app.root.state.startDestinationChoice
import com.luum.michi.app.root.state.toDashboardMediaType
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * "Pestaña al abrir" resolution: pure, so it is tested without the UI. A guest
 * can only land on DISCOVER and a section id that no longer exists (split
 * toggled off, or the enum renamed) falls back to ALL.
 */
class StartDestinationTest {

    private fun resolve(
        tab: StartTabOption = StartTabOption.DISCOVER,
        animeSection: String = "ALL",
        mangaSection: String = "ALL",
        splitAnime: Boolean = true,
        splitManga: Boolean = true,
        isGuest: Boolean = false,
    ) = resolveStartDestination(
        startTab = tab,
        animeSectionId = animeSection,
        mangaSectionId = mangaSection,
        splitCompletedAnime = splitAnime,
        splitCompletedManga = splitManga,
        isGuest = isGuest,
    )

    @Test
    fun defaultsAreDiscoverWithAllSections() {
        val destination = resolve()

        assertEquals(TabSection.DISCOVER, destination.tab)
        assertEquals(AnimeListSection.ALL, destination.animeSection)
        assertEquals(MangaListSection.ALL, destination.mangaSection)
        assertEquals(DashboardMediaType.ANIME, StartCatalogOption.ANIME.toDashboardMediaType())
    }

    @Test
    fun aGuestFallsBackToDiscover() {
        assertEquals(TabSection.DISCOVER, resolve(tab = StartTabOption.ANIME, isGuest = true).tab)
        assertEquals(TabSection.DISCOVER, resolve(tab = StartTabOption.MANGA, isGuest = true).tab)
        assertEquals(TabSection.DISCOVER, resolve(tab = StartTabOption.PROFILE, isGuest = true).tab)
        // A signed-in user keeps the chosen tab.
        assertEquals(TabSection.ANIME, resolve(tab = StartTabOption.ANIME).tab)
    }

    @Test
    fun aSectionThatNoLongerExistsFallsBackToAll() {
        // COMPLETED_TV only exists while "split completed" is on.
        assertEquals(
            AnimeListSection.COMPLETED_TV,
            resolve(animeSection = "COMPLETED_TV", splitAnime = true).animeSection,
        )
        assertEquals(
            AnimeListSection.ALL,
            resolve(animeSection = "COMPLETED_TV", splitAnime = false).animeSection,
        )
        assertEquals(
            MangaListSection.COMPLETED_NOVEL,
            resolve(mangaSection = "COMPLETED_NOVEL", splitManga = true).mangaSection,
        )
        assertEquals(
            MangaListSection.ALL,
            resolve(mangaSection = "COMPLETED_NOVEL", splitManga = false).mangaSection,
        )
    }

    @Test
    fun aRenamedOrUnknownSectionFallsBackToAll() {
        assertEquals(AnimeListSection.ALL, resolve(animeSection = "NOT_A_SECTION").animeSection)
        assertEquals(MangaListSection.ALL, resolve(mangaSection = "NOT_A_SECTION").mangaSection)
    }

    @Test
    fun theCatalogSubChoiceSelectsTheDashboardType() {
        assertEquals(
            DashboardMediaType.MANGA,
            StartCatalogOption.MANGA.toDashboardMediaType(),
        )
    }

    // ── startDestinationChoice: the localized settings picker ──

    private fun choice(
        tab: StartTabOption = StartTabOption.ANIME,
        catalog: StartCatalogOption = StartCatalogOption.ANIME,
        animeSection: String = "ALL",
        mangaSection: String = "ALL",
        splitAnime: Boolean = true,
        splitManga: Boolean = true,
    ) = startDestinationChoice(
        tab = tab,
        catalogType = catalog,
        animeSectionId = animeSection,
        mangaSectionId = mangaSection,
        splitCompletedAnime = splitAnime,
        splitCompletedManga = splitManga,
        strings = EnglishStrings,
    )

    @Test
    fun anUnknownSectionIdShowsTheFirstOption() {
        assertEquals("ALL", choice(animeSection = "NOT_A_SECTION").sectionValue)
        assertEquals("ALL", choice(tab = StartTabOption.MANGA, mangaSection = "NOT_A_SECTION").sectionValue)
    }

    @Test
    fun profileHasNoSubChoice() {
        val profile = choice(tab = StartTabOption.PROFILE)

        assertTrue(profile.sectionChoices.isEmpty())
        assertEquals("", profile.sectionValue)
        assertNull(profile.sectionTitle)
    }

    @Test
    fun splitOffHidesThePerFormatSections() {
        val animeSplit = choice(splitAnime = true).sectionChoices.map { it.value }
        assertTrue("COMPLETED_TV" in animeSplit)
        val animeMerged = choice(splitAnime = false).sectionChoices.map { it.value }
        assertTrue("COMPLETED" in animeMerged)
        assertTrue("COMPLETED_TV" !in animeMerged)

        val mangaSplit = choice(tab = StartTabOption.MANGA, splitManga = true).sectionChoices.map { it.value }
        assertTrue("COMPLETED_NOVEL" in mangaSplit)
        val mangaMerged = choice(tab = StartTabOption.MANGA, splitManga = false).sectionChoices.map { it.value }
        assertTrue("COMPLETED" in mangaMerged)
        assertTrue("COMPLETED_NOVEL" !in mangaMerged)
    }

    @Test
    fun aStoredPerFormatSectionIsKeptOnlyWhileSplitIsOn() {
        assertEquals("COMPLETED_TV", choice(animeSection = "COMPLETED_TV", splitAnime = true).sectionValue)
        // Same stored id, but the section no longer exists: show the fallback.
        assertEquals("ALL", choice(animeSection = "COMPLETED_TV", splitAnime = false).sectionValue)
    }
}
