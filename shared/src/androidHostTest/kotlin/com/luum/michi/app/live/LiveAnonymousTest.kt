package com.luum.michi.app.live

import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListRateLimiter
import com.luum.michi.app.core.network.repository.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.repository.createAniListHttpClient
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.mediaDetail.repository.media.MediaDetailRepositoryImpl
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/**
 * Live checks against the real AniList API (anonymous, no token).
 * Opt-in only: run explicitly with LIVE_API_CHECK=1, never in CI.
 *   LIVE_API_CHECK=1 ./gradlew :shared:testAndroidHostTest --tests "com.luum.michi.app.live.*"
 * Asserts are structural (connectivity + mapping), never data values.
 */
private fun assumeLive() {
    assumeTrue(
        "Set LIVE_API_CHECK=1 to run live API checks",
        System.getenv("LIVE_API_CHECK") != null,
    )
}

private fun liveClient() = KtorAniListGraphQLClient(
    httpClient = createAniListHttpClient(engine = OkHttp.create()),
    tokenProvider = { null },
    rateLimiter = AniListRateLimiter(),
)

class LiveAnonymousTest {

    @Test
    fun dashboardLoads() {
        assumeLive()
        val result = runBlocking { DashboardRepositoryImpl(liveClient()).loadFeed(EnglishStrings) }
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).value.trendingAnime.isNotEmpty())
    }

    @Test
    fun exploreSearches() {
        assumeLive()
        val result = runBlocking {
            ExploreRepositoryImpl(liveClient()).searchCatalog(
                query = "Naruto",
                genres = emptyList(),
                formats = emptyList(),
                year = null,
                sort = "SEARCH_MATCH",
                page = 1,
                strings = EnglishStrings,
            )
        }
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).value.results.isNotEmpty())
    }

    @Test
    fun calendarLoads() {
        assumeLive()
        val result = runBlocking { CalendarRepositoryImpl(liveClient()).loadFeed().first() }
        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun mediaDetailLoads() {
        assumeLive()
        val result = runBlocking {
            MediaDetailRepositoryImpl(liveClient()).loadDetail(
                mediaId = 1,
                voiceLanguage = "JAPANESE",
                strings = EnglishStrings,
            )
        }
        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).value.title.isNotBlank())
    }
}
