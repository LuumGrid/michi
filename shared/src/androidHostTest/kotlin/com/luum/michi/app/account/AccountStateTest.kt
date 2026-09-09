package com.luum.michi.app.account

import com.luum.michi.app.account.domain.AccountData
import com.luum.michi.app.account.domain.AccountFavoritesPage
import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.domain.model.AccountStats
import com.luum.michi.app.account.repository.toAccountMediaTypeStats
import com.luum.michi.app.account.domain.model.toCompactCountLabel
import com.luum.michi.app.account.ui.state.AccountStateHolder
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.repository.anilist.dto.UserStatisticsDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountStateTest {

    @Test
    fun mediaTypeStatsPicksEpisodesOrChapters() {
        val dto = UserStatisticsDto(
            episodesWatched = 100,
            minutesWatched = 2880,
            chaptersRead = 50,
            volumesRead = 7,
        )
        val anime = dto.toAccountMediaTypeStats(isManga = false)
        assertEquals(100, anime.episodesOrChapters)
        assertEquals(2.0, anime.daysOrVolumes)
        val manga = dto.toAccountMediaTypeStats(isManga = true)
        assertEquals(50, manga.episodesOrChapters)
        assertEquals(7.0, manga.daysOrVolumes)
    }

    @Test
    fun compactCountLabelBoundaries() {
        assertEquals("999", 999.toCompactCountLabel())
        assertEquals("1.0K", 1000.toCompactCountLabel())
        assertEquals("1.5K", 1500.toCompactCountLabel())
        assertEquals("10K", 10000.toCompactCountLabel())
        assertEquals("1.5M", 1500000.toCompactCountLabel())
    }

    @Test
    fun overlappingLoadIssuesASingleFetch() {
        val gate = CompletableDeferred<Unit>()
        var fetches = 0
        val repository = object : AccountRepository {
            override suspend fun loadAccount(userId: Int): NetworkResult<AccountData> {
                fetches++
                gate.await()
                return NetworkResult.Success(
                    AccountData(
                        stats = AccountStats(1, 2, 3, 4),
                        favorites = AccountFavorites.EMPTY,
                    ),
                )
            }

            override suspend fun loadFavoritesPage(
                userId: Int,
                category: AccountFavoritesCategory,
                page: Int,
            ): NetworkResult<AccountFavoritesPage> = throw NotImplementedError()
        }
        val holder = AccountStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load(1)
        holder.load(1)
        gate.complete(Unit)
        assertEquals(1, fetches)
        assertEquals(1, holder.stats.animeCount)
    }
}
