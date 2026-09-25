package com.luum.michi.app.profile

import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileRepositoryLoadTest {

    @Test
    fun loadProfileMapsCountsFavoritesAndFollows() {
        val result = runBlocking { ProfileRepositoryImpl(FakeProfileGraphQL(listOf(profileData()))).loadProfile(1) }
        assertTrue(result is NetworkResult.Success)
        assertEquals(10, result.value.stats.animeCount)
        assertEquals(5, result.value.stats.mangaCount)
        assertEquals(7, result.value.stats.followersCount)
        assertEquals(9, result.value.stats.followingCount)
        assertEquals("A", result.value.favorites.anime.single().title)
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
        assertEquals("A", result.value.mediaItems.single().title)
        assertTrue(result.value.hasNextPage)
    }

    @Test
    fun loadFavoritesPageMapsPersonBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("characters", "C", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.CHARACTERS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("C", result.value.personItems.single().name)
    }

    @Test
    fun loadFavoritesPageMapsMangaBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("manga", "M", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.MANGA, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("M", result.value.mediaItems.single().title)
    }

    @Test
    fun loadFavoritesPageMapsStaffBranch() {
        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("staff", "S", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.STAFF, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("S", result.value.personItems.single().name)
    }

    @Test
    fun loadFavoritesPageMapsStudioBranch() {        val result = runBlocking {
            ProfileRepositoryImpl(FakeProfileGraphQL(listOf(favouritesPage("studios", "S", false))))
                .loadFavoritesPage(1, ProfileFavoritesCategory.STUDIOS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("S", result.value.studioItems.single().name)
    }
}
