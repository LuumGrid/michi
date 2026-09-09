package com.luum.michi.app.account

import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.repository.AccountRepositoryImpl
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountRepositoryLoadTest {

    @Test
    fun loadAccountMapsCountsFavoritesAndFollows() {
        val result = runBlocking { AccountRepositoryImpl(FakeAccountGraphQL(listOf(accountData()))).loadAccount(1) }
        assertTrue(result is NetworkResult.Success)
        assertEquals(10, result.value.stats.animeCount)
        assertEquals(5, result.value.stats.mangaCount)
        assertEquals(7, result.value.stats.followersCount)
        assertEquals(9, result.value.stats.followingCount)
        assertEquals("A", result.value.favorites.anime.single().title)
        assertTrue(result.value.favorites.manga.isEmpty())
    }

    @Test
    fun loadAccountWithNullUserFallsBackToEmpty() {
        val result = runBlocking {
            AccountRepositoryImpl(FakeAccountGraphQL(listOf(buildJsonObject {}))).loadAccount(1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals(0, result.value.stats.animeCount)
        assertTrue(result.value.favorites.anime.isEmpty())
        assertTrue(result.value.favorites.studios.isEmpty())
    }

    @Test
    fun loadFavoritesPageMapsMediaBranch() {
        val result = runBlocking {
            AccountRepositoryImpl(FakeAccountGraphQL(listOf(favouritesPage("anime", "A", true))))
                .loadFavoritesPage(1, AccountFavoritesCategory.ANIME, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("A", result.value.mediaItems.single().title)
        assertTrue(result.value.hasNextPage)
    }

    @Test
    fun loadFavoritesPageMapsPersonBranch() {
        val result = runBlocking {
            AccountRepositoryImpl(FakeAccountGraphQL(listOf(favouritesPage("characters", "C", false))))
                .loadFavoritesPage(1, AccountFavoritesCategory.CHARACTERS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("C", result.value.personItems.single().name)
    }

    @Test
    fun loadFavoritesPageMapsStudioBranch() {
        val result = runBlocking {
            AccountRepositoryImpl(FakeAccountGraphQL(listOf(favouritesPage("studios", "S", false))))
                .loadFavoritesPage(1, AccountFavoritesCategory.STUDIOS, 1)
        }
        assertTrue(result is NetworkResult.Success)
        assertEquals("S", result.value.studioItems.single().name)
    }
}
