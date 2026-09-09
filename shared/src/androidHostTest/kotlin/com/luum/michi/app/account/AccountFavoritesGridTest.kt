package com.luum.michi.app.account

import com.luum.michi.app.account.domain.AccountData
import com.luum.michi.app.account.domain.AccountFavoritesPage
import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.account.domain.model.AccountFavoriteMedia
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.account.ui.state.AccountFavoritesGridStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeGridRepository : AccountRepository {
    var failNext = false
    var gate: CompletableDeferred<Unit>? = null
    val calls = mutableListOf<Triple<Int, AccountFavoritesCategory, Int>>()

    override suspend fun loadAccount(userId: Int): NetworkResult<AccountData> =
        throw NotImplementedError()

    override suspend fun loadFavoritesPage(
        userId: Int,
        category: AccountFavoritesCategory,
        page: Int,
    ): NetworkResult<AccountFavoritesPage> {
        calls += Triple(userId, category, page)
        gate?.await()
        if (failNext) {
            failNext = false
            return NetworkResult.Failure(NetworkError.Unknown(null))
        }
        val item = AccountFavoriteMedia(
            id = page * 100 + category.ordinal,
            title = "Title",
            coverUrl = null,
            paletteHex = null,
        )
        return NetworkResult.Success(
            AccountFavoritesPage(mediaItems = listOf(item), hasNextPage = page == 1),
        )
    }
}

class AccountFavoritesGridTest {

    @Test
    fun retryAfterFirstPageFailureRefetches() {
        val repository = FakeGridRepository().apply { failNext = true }
        val holder = AccountFavoritesGridStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load(1, AccountFavoritesCategory.ANIME)
        assertTrue(holder.error is NetworkError.Unknown)
        assertTrue(holder.mediaItems.isEmpty())
        holder.load(1, AccountFavoritesCategory.ANIME)
        assertNull(holder.error)
        assertEquals(1, holder.mediaItems.size)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun loadMoreAppendsNextPageAndAdvances() {
        val repository = FakeGridRepository()
        val holder = AccountFavoritesGridStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load(1, AccountFavoritesCategory.ANIME)
        assertEquals(true, holder.hasNextPage)
        holder.loadMore()
        assertEquals(listOf(100, 200), holder.mediaItems.map { it.id })
        assertEquals(false, holder.hasNextPage)
    }

    @Test
    fun categorySwitchReloads() {
        val repository = FakeGridRepository()
        val holder = AccountFavoritesGridStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load(1, AccountFavoritesCategory.ANIME)
        holder.load(1, AccountFavoritesCategory.MANGA)
        assertEquals(1, holder.mediaItems.size)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun loadCancelsInFlightLoadMore() {        val repository = FakeGridRepository()
        val holder = AccountFavoritesGridStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load(1, AccountFavoritesCategory.ANIME)
        repository.gate = CompletableDeferred()
        holder.loadMore()
        repository.gate = null
        holder.load(1, AccountFavoritesCategory.MANGA)
        assertEquals(
            listOf(101),
            holder.mediaItems.map { it.id },
        )
        assertEquals(false, holder.isLoadingMore)
    }
}
