package com.luum.michi.app.profile

import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Collapses whitespace so a query-shape assertion is independent of indentation. */
private fun normalized(query: String) = query.replace(Regex("\\s+"), " ").trim()

class ProfileRepositoryLoadTest {

    @Test
    fun loadProfileMapsCountsFavoritesAndFollows() {
        val result = runBlocking { ProfileRepositoryImpl(FakeProfileGraphQL(listOf(profileData()))).loadProfile(1) }
        assertTrue(result is NetworkResult.Success)
        assertEquals(10, result.value.stats.animeCount)
        assertEquals(5, result.value.stats.mangaCount)
        assertEquals(7, result.value.stats.followersCount)
        assertEquals(9, result.value.stats.followingCount)
        assertEquals("A", result.value.favorites.anime.single().name)
        assertTrue(result.value.favorites.manga.isEmpty())
    }

    @Test
    fun loadProfileWithNullUserFallsBackToEmpty() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(buildJsonObject {}))).loadProfile(1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals(0, result.value.stats.animeCount)
        assertTrue(result.value.favorites.anime.isEmpty())
        assertTrue(result.value.favorites.studios.isEmpty())
    }

    @Test
    fun loadFavoritesPageMapsMediaBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("anime", "A", true))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.ANIME, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("A", result.value.items.single().name)
        assertTrue(result.value.hasNextPage)
    }

    @Test
    fun loadFavoritesPageMapsPersonBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("characters", "C", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.CHARACTERS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("C", result.value.items.single().name)
    }

    @Test
    fun loadFavoritesPageMapsMangaBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("manga", "M", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.MANGA, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("M", result.value.items.single().name)
    }

    @Test
    fun loadFavoritesPageMapsStaffBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("staff", "S", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.STAFF, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("S", result.value.items.single().name)
    }

    @Test
    fun loadFavoritesPageMapsStudioBranchWithoutAnImage() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("studios", "S", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.STUDIOS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        val studio = result.value.items.single()
        assertEquals("S", studio.name)
        assertNull(studio.image, "a studio has no image on AniList")
        assertNull(studio.paletteHex)
    }

    @Test
    fun profileAndPersonFavoritesAskForTheSecondLineFields() {
        val client = FakeProfileGraphQL(
            listOf(
                profileData(),
                favouritesPage("characters", "C", false),
                favouritesPage("staff", "S", false),
            ),
        )
        val repository = ProfileRepositoryImpl(client)
        runBlocking {
            repository.loadProfile(1)
            repository.loadFavoritesPage(1, ProfileFavoritesCategory.CHARACTERS, 1)
            repository.loadFavoritesPage(1, ProfileFavoritesCategory.STAFF, 1)
        }

        val mediaFields = "media(sort: [POPULARITY_DESC], perPage: 1) { " +
            "nodes { id title { romaji english native userPreferred } } }"
        val profile = normalized(client.requests[0].query)
        assertTrue(profile.contains(mediaFields), "the profile asks for the character's media")
        assertTrue(profile.contains("primaryOccupations"), "the profile asks for staff occupations")
        assertTrue(profile.contains("startDate { year }"), "the profile asks for the media year")

        assertTrue(
            normalized(client.requests[1].query).contains(mediaFields),
            "the characters page asks for the character's media",
        )
        assertTrue(
            normalized(client.requests[2].query).contains("primaryOccupations"),
            "the staff page asks for staff occupations",
        )
    }

    /**
     * Tripwire: the studios selection is exactly `{ id name }` — the old
     * `media { nodes { coverImage } }` trick must never come back. Asserted on the
     * exact normalized block, not a loose `!contains("media")` over the query.
     */
    @Test
    fun studioQueriesNeverAskForMedia() {
        val client = FakeProfileGraphQL(listOf(profileData(), favouritesPage("studios", "S", false)))
        val repository = ProfileRepositoryImpl(client)
        runBlocking {
            repository.loadProfile(1)
            repository.loadFavoritesPage(1, ProfileFavoritesCategory.STUDIOS, 1)
        }

        assertTrue(
            normalized(client.requests[0].query)
                .contains("studios(perPage: 12) { nodes { id name } }"),
            "the profile studios selection must be only id + name",
        )
        assertTrue(
            normalized(client.requests[1].query)
                .contains(
                    "studios(page: \$page, perPage: \$perPage) { " +
                        "pageInfo { hasNextPage currentPage } nodes { id name } }",
                ),
            "the paged studios selection must be only id + name",
        )
    }
}
