package com.luum.michi.app.core

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.ExternalLinkCatalogRepositoryImpl
import com.luum.michi.app.settings.FakeSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun catalogJson(vararg sites: Pair<String, String>): JsonElement = buildJsonObject {
    put(
        "ExternalLinkSourceCollection",
        JsonArray(
            sites.map { (site, url) ->
                buildJsonObject {
                    put("site", site)
                    put("url", url)
                    put("icon", "$url/favicon.ico")
                    put("color", "#FF0000")
                    put("type", "SOCIAL")
                }
            },
        ),
    )
}

private class CountingCatalogClient(
    private val pages: List<JsonElement>,
    private val failureAt: Int = -1,
) : AniListGraphQLClient {
    var calls = 0

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val index = calls++
        if (index == failureAt) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(parseData(pages[index]))
    }
}

/** Blocks every request until [release], to prove callers share one request. */
private class GatedCatalogClient(private val page: JsonElement) : AniListGraphQLClient {
    var calls = 0
    private val gate = CompletableDeferred<Unit>()

    fun release() = gate.complete(Unit)

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        calls++
        gate.await()
        return NetworkResult.Success(parseData(page))
    }
}

private fun repo(
    client: AniListGraphQLClient,
    store: FakeSettingsStore = FakeSettingsStore(),
    now: () -> Long = { 1_000L },
) = ExternalLinkCatalogRepositoryImpl(
    graphQLClient = client,
    store = store,
    scope = CoroutineScope(Dispatchers.Unconfined),
    now = now,
)

class ExternalLinkCatalogRepositoryTest {

    @Test
    fun failedFetchIsRememberedForTenMinutes() = runBlocking {
        var clock = 1_000L
        val client = CountingCatalogClient(
            pages = listOf(
                catalogJson("Twitter" to "https://twitter.com"),
                catalogJson("Twitter" to "https://twitter.com"),
            ),
            failureAt = 0,
        )
        val catalog = repo(client, now = { clock })

        assertTrue(catalog.sites().isEmpty())
        assertTrue(catalog.sites().isEmpty())
        assertEquals(1, client.calls, "the failure is remembered, no retry")

        clock += 10L * 60 * 1000 + 1
        catalog.sites()
        assertEquals(2, client.calls, "the negative cache expired")
    }

    @Test
    fun successIsCachedInMemoryAndOnDisk() = runBlocking {
        val client = CountingCatalogClient(listOf(catalogJson("Twitter" to "https://twitter.com")))
        val store = FakeSettingsStore()
        val catalog = repo(client, store)

        assertEquals("Twitter", catalog.sites().single().site)
        assertEquals(1, client.calls)

        catalog.sites() // memory hit
        assertEquals(1, client.calls)

        // A fresh repository over the same store reads the disk cache.
        assertEquals("Twitter", repo(client, store).sites().single().site)
        assertEquals(1, client.calls)
    }

    @Test
    fun concurrentCallersShareOneRequest() = runBlocking {
        val client = GatedCatalogClient(catalogJson("Twitter" to "https://twitter.com"))
        val catalog = repo(client)

        catalog.prefetch() // starts the request, suspended on the gate
        catalog.prefetch() // already in flight: no second request
        val first = async(Dispatchers.Unconfined) { catalog.sites() }
        val second = async(Dispatchers.Unconfined) { catalog.sites() }
        assertEquals(1, client.calls, "the callers joined the in-flight request")

        client.release()
        assertEquals("Twitter", first.await().single().site)
        assertEquals("Twitter", second.await().single().site)
        assertEquals(1, client.calls)
    }
}
