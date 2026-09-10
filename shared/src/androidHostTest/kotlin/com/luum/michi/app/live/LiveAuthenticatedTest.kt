package com.luum.michi.app.live

import com.luum.michi.app.account.repository.AccountRepositoryImpl
import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.InMemoryAniListTokenStorage
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListRateLimiter
import com.luum.michi.app.core.network.repository.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.repository.createAniListHttpClient
import com.luum.michi.app.core.session.repository.AniListViewerRepositoryImpl
import com.luum.michi.app.core.session.domain.SessionManager
import com.luum.michi.app.core.session.domain.SessionState
import com.luum.michi.app.core.session.domain.loadIfAuthenticated
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.repository.anime.AnimeListRepositoryImpl
import com.luum.michi.app.mediaList.repository.manga.MangaListRepositoryImpl
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live checks with a real user token (opt-in only, never CI).
 * Token source: `anilistTestToken` in local.properties (gitignored),
 * injected as ANILIST_TEST_TOKEN env. Run explicitly:
 *   ./gradlew :shared:testAndroidHostTest --tests "com.luum.michi.app.live.*"
 * with LIVE_API_CHECK=1 also set (anonymous suite shares the filter).
 * Asserts are structural, never data values. Never log the token.
 */
private fun requireToken(): String =
    System.getenv("ANILIST_TEST_TOKEN")?.takeIf { it.isNotBlank() }
        ?: error("Missing ANILIST_TEST_TOKEN (set anilistTestToken in local.properties)")

private class LiveStack(private val token: String) {
    private val storage = InMemoryAniListTokenStorage()
    private val graphQLClient = KtorAniListGraphQLClient(
        httpClient = createAniListHttpClient(engine = OkHttp.create()),
        tokenProvider = { storage.load() },
        rateLimiter = AniListRateLimiter(),
    )
    private val viewerRepository = AniListViewerRepositoryImpl(graphQLClient)
    val sessionManager = SessionManager(storage, viewerRepository)
    val animeListRepository: AnimeListRepository = AnimeListRepositoryImpl(graphQLClient)
    val mangaListRepository: MangaListRepository = MangaListRepositoryImpl(graphQLClient)
    val accountRepository = AccountRepositoryImpl(graphQLClient)

    val rawToken: String = token

    suspend fun bootstrapAuthenticated(): Int {
        storage.save(AniListToken(accessToken = token, savedAtEpochSeconds = currentEpochSeconds()))
        sessionManager.bootstrap()
        val state = sessionManager.state.value
        assertTrue(state is SessionState.Authenticated, "expected Authenticated but was $state")
        return state.viewer.id
    }
}

class LiveAuthenticatedTest {

    @Test
    fun bootstrapAuthenticates() {
        val stack = LiveStack(requireToken())
        runBlocking {
            val viewerId = stack.bootstrapAuthenticated()
            assertTrue(viewerId > 0)
        }
    }

    @Test
    fun gatedAnimeListLoads() {
        val stack = LiveStack(requireToken())
        runBlocking {
            val viewerId = stack.bootstrapAuthenticated()
            val loaded = loadIfAuthenticated(stack.sessionManager.state.value) { id ->
                assertTrue(stack.animeListRepository.loadList(id) is NetworkResult.Success)
            }
            assertTrue(loaded)
        }
    }

    @Test
    fun gatedMangaListLoads() {
        val stack = LiveStack(requireToken())
        runBlocking {
            val viewerId = stack.bootstrapAuthenticated()
            val loaded = loadIfAuthenticated(stack.sessionManager.state.value) { id ->
                assertTrue(stack.mangaListRepository.loadList(id) is NetworkResult.Success)
            }
            assertTrue(loaded)
        }
    }

    @Test
    fun gatedAccountLoads() {
        val stack = LiveStack(requireToken())
        runBlocking {
            val viewerId = stack.bootstrapAuthenticated()
            val loaded = loadIfAuthenticated(stack.sessionManager.state.value) { id ->
                assertTrue(stack.accountRepository.loadAccount(id) is NetworkResult.Success)
            }
            assertTrue(loaded)
        }
    }
}
