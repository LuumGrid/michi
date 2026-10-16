package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.favourite.repository.FavouriteRepositoryImpl
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.repository.ExternalLinkCatalogRepositoryImpl
import com.luum.michi.app.mediaDetail.repository.character.CharacterDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.repository.staff.StaffDetailRepositoryImpl
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

private const val CatalogOperation = "ExternalLinkCatalog"

private fun staffJson(description: String?): JsonElement = buildJsonObject {
    put("Staff", buildJsonObject {
        put("id", 1)
        if (description != null) put("description", description)
    })
}

private fun characterJson(description: String?): JsonElement = buildJsonObject {
    put("Character", buildJsonObject {
        put("id", 1)
        if (description != null) put("description", description)
    })
}

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

/** Records request order and blocks the catalog until [releaseCatalog]. */
private class CatalogProbeClient(
    private val detailJson: JsonElement,
    private val catalogResponseJson: JsonElement,
) : AniListGraphQLClient {
    val order = mutableListOf<String>()
    var catalogPending = false
    private val gate = CompletableDeferred<Unit>()

    fun releaseCatalog() = gate.complete(Unit)

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> =
        if (request.operationName == CatalogOperation) {
            order += "catalog"
            catalogPending = true
            gate.await()
            catalogPending = false
            NetworkResult.Success(parseData(catalogResponseJson))
        } else {
            order += "detail"
            NetworkResult.Success(parseData(detailJson))
        }
}

private fun catalog(client: AniListGraphQLClient) = ExternalLinkCatalogRepositoryImpl(
    graphQLClient = client,
    store = FakeSettingsStore(),
    scope = CoroutineScope(Dispatchers.Unconfined),
)

class PersonLinkCatalogIntegrationTest {

    @Test
    fun detailWithoutLinksDoesNotWaitForTheCatalog() = runBlocking {
        val client = CatalogProbeClient(staffJson("Voice actor."), catalogJson("Fanbox" to "https://fanbox.cc"))
        val catalogRepo = catalog(client)
        val repo = StaffDetailRepositoryImpl(client, catalogRepo, FavouriteRepositoryImpl(client))

        val detailJob = async(Dispatchers.Unconfined) { repo.loadDetail(1, EnglishStrings) }

        assertTrue(client.catalogPending, "the catalog fetch runs in its own scope")
        assertTrue(detailJob.isCompleted, "a detail without links never waits for the catalog")
        assertTrue(detailJob.await() is NetworkResult.Success)

        client.releaseCatalog()
        // The prefetch still lands in the cache for the next detail.
        assertEquals(1, catalogRepo.sites().size)
        assertEquals(1, client.order.count { it == "catalog" })
    }

    @Test
    fun biographyLinksAreEnrichedFromTheCatalog() = runBlocking {
        val client = CatalogProbeClient(
            detailJson = staffJson("Voice actor.<br><a href=\"https://usuario.fanbox.cc\">@usuario</a>"),
            catalogResponseJson = catalogJson("Fanbox" to "https://fanbox.cc"),
        )
        val repo = StaffDetailRepositoryImpl(client, catalog(client), FavouriteRepositoryImpl(client))

        val detailJob = async(Dispatchers.Unconfined) { repo.loadDetail(1, EnglishStrings) }
        client.releaseCatalog()
        val result = detailJob.await()

        assertTrue(result is NetworkResult.Success)
        val link = result.value.links.single()
        assertEquals("Fanbox", link.site, "the catalog name replaces the host-derived label")
        assertEquals("https://usuario.fanbox.cc", link.url)
        assertEquals("@usuario", link.handle)
        assertEquals("https://fanbox.cc/favicon.ico", link.iconUrl)
        assertEquals("#FF0000", link.colorHex)
        assertEquals("Voice actor.", result.value.descriptionPlain)
    }

    @Test
    fun catalogIsFetchedOnceAcrossDetails() = runBlocking {
        val client = CatalogProbeClient(staffJson("Voice actor."), catalogJson("Fanbox" to "https://fanbox.cc"))
        val repo = StaffDetailRepositoryImpl(client, catalog(client), FavouriteRepositoryImpl(client))

        val first = async(Dispatchers.Unconfined) { repo.loadDetail(1, EnglishStrings) }
        client.releaseCatalog()
        assertTrue(first.await() is NetworkResult.Success)

        assertTrue(repo.loadDetail(1, EnglishStrings) is NetworkResult.Success, "warm catalog")

        assertEquals(1, client.order.count { it == "catalog" })
        assertEquals(2, client.order.count { it == "detail" })
    }

    @Test
    fun characterBiographyLinksUseTheSameCatalog() = runBlocking {
        val client = CatalogProbeClient(
            detailJson = characterJson("Seiyuu.<br><a href=\"https://x.com/voice\">@voice</a>"),
            catalogResponseJson = catalogJson("Twitter" to "https://twitter.com"),
        )
        val repo = CharacterDetailRepositoryImpl(client, catalog(client), FavouriteRepositoryImpl(client))

        val detailJob = async(Dispatchers.Unconfined) { repo.loadDetail(1, "JAPANESE", EnglishStrings) }
        client.releaseCatalog()
        val result = detailJob.await()

        assertTrue(result is NetworkResult.Success)
        val link = result.value.links.single()
        assertEquals("Twitter", link.site)
        assertEquals("@voice", link.handle)
    }
}
