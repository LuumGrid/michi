package com.luum.michi.app.discover

import com.luum.michi.app.core.language.EnglishStrings
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.discover.data.ExploreRepositoryImpl
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExplorePage
import com.luum.michi.app.discover.presentation.explore.state.ExploreStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val EMPTY_PAGE = """{"Page":{"pageInfo":{"hasNextPage":false},"media":[]}}"""

private class FakeGraphQLClient : AniListGraphQLClient {
    val requests = mutableListOf<AniListGraphQLRequest>()
    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        requests += request
        return NetworkResult.Success(parseData(AniListJson.parseToJsonElement(EMPTY_PAGE)))
    }
}

private data class CatalogCall(
    val query: String?,
    val genres: List<String>,
    val formats: List<String>,
    val year: Int?,
    val sort: String,
    val season: String?,
)

private class FakeExploreRepository : ExploreRepository {
    val catalogCalls = mutableListOf<CatalogCall>()
    val mangaCalls = mutableListOf<CatalogCall>()
    private fun empty() = NetworkResult.Success(ExplorePage(emptyList(), false))

    override suspend fun searchCatalog(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        strings: com.luum.michi.app.core.language.LanguageStrings,
    ): NetworkResult<ExplorePage> {
        catalogCalls += CatalogCall(query, genres, formats, year, sort, season)
        return empty()
    }

    override suspend fun searchManga(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        onList: Boolean?,
        strings: com.luum.michi.app.core.language.LanguageStrings,
    ): NetworkResult<ExplorePage> {
        mangaCalls += CatalogCall(query, genres, formats, year, sort, season = null)
        return empty()
    }

    override suspend fun searchCharacters(query: String?, page: Int, perPage: Int) = empty()
    override suspend fun searchStaff(query: String?, page: Int, perPage: Int) = empty()
    override suspend fun searchStudios(query: String?, page: Int, perPage: Int) = empty()
}

private fun varsOf(request: AniListGraphQLRequest): Map<String, String> =
    request.variables?.entries?.associate { (k, v) -> k to v.toString() }.orEmpty()

class ExploreFilterTest {

    // ---- Holder -> repository wiring (caso exacto reportado) ----

    @Test
    fun applyPresetForwardsUserCase() = runBlocking {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), EnglishStrings)
        holder.applyPreset(
            category = ExploreCategory.ANIME,
            sortOption = UserListSort.TRENDING,
            season = null,
            year = 2025,
        )
        // applyPreset es sin debounce: la llamada ya debe estar registrada
        assertEquals(1, repo.catalogCalls.size)
        val call = repo.catalogCalls.single()
        assertNull(call.query)
        assertEquals(emptyList(), call.genres)
        assertEquals(emptyList(), call.formats)
        assertEquals(2025, call.year)
        assertNull(call.season)
        assertEquals("TRENDING_DESC", call.sort)
    }

    @Test
    fun updateFiltersForwardsGenresFormatsAndYear() {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), EnglishStrings)
        holder.updateFilters(
            newCategory = ExploreCategory.ANIME,
            newSeason = null,
            newGenres = listOf("Adventure", "Comedy"),
            newFormats = listOf("TV", "MOVIE"),
            newYear = 2025,
        )
        // Estado síncrono: el sheet ya refleja el draft aplicado
        assertEquals(listOf("Adventure", "Comedy"), holder.genres)
        assertEquals(listOf("TV", "MOVIE"), holder.formats)
        assertEquals(2025, holder.year)
        // La query sale tras el debounce de 300ms
        val deadline = System.currentTimeMillis() + 3000
        while (repo.catalogCalls.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(1, repo.catalogCalls.size)
        val call = repo.catalogCalls.single()
        assertEquals(listOf("Adventure", "Comedy"), call.genres)
        assertEquals(listOf("TV", "MOVIE"), call.formats)
        assertEquals(2025, call.year)
        assertNull(call.season)
    }

    @Test
    fun mangaSearchIgnoresSeason() = runBlocking {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), EnglishStrings)
        holder.applyPreset(ExploreCategory.MANGA, UserListSort.TRENDING, year = 2025)
        assertEquals(1, repo.mangaCalls.size)
        assertEquals(2025, repo.mangaCalls.single().year)
        assertTrue(repo.catalogCalls.isEmpty())
    }

    // ---- Repository -> variables GraphQL exactas ----

    @Test
    fun catalogVariablesMatchUserCase() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchCatalog(
            query = null,
            genres = listOf("Adventure", "Comedy"),
            formats = listOf("TV", "MOVIE"),
            year = 2025,
            sort = "TRENDING_DESC",
            season = null,
            onList = null,
            strings = EnglishStrings,
        )
        assertEquals(1, client.requests.size)
        val vars = varsOf(client.requests.single())
        assertEquals("[\"Adventure\",\"Comedy\"]", vars["genre_in"])
        assertEquals("[\"TV\",\"MOVIE\"]", vars["format_in"])
        assertEquals("2025", vars["seasonYear"])
        assertEquals("[\"TRENDING_DESC\"]", vars["sort"])
        assertEquals(null, vars["season"])
        assertEquals(null, vars["search"])
        assertEquals("AnimeCatalog", client.requests.single().operationName)
    }

    @Test
    fun mangaYearUsesFuzzyDateBounds() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchManga(
            query = null,
            genres = emptyList(),
            formats = emptyList(),
            year = 2025,
            sort = "TRENDING_DESC",
            onList = null,
            strings = EnglishStrings,
        )
        val vars = varsOf(client.requests.single())
        assertEquals("20241231", vars["startDate_greater"])
        assertEquals("20260101", vars["startDate_lesser"])
        assertEquals(null, vars["seasonYear"])
    }

    @Test
    fun blankFiltersOmitOptionalVariables() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchCatalog(
            query = "  ",
            genres = emptyList(),
            formats = emptyList(),
            year = null,
            sort = "TRENDING_DESC",
            season = null,
            onList = null,
            strings = EnglishStrings,
        )
        val vars = varsOf(client.requests.single())
        assertEquals(null, vars["genre_in"])
        assertEquals(null, vars["format_in"])
        assertEquals(null, vars["seasonYear"])
        assertEquals(null, vars["season"])
        assertEquals(null, vars["search"])
    }

    @Test
    fun formatValuesNormalizeToApiEnum() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchCatalog(
            query = null,
            genres = emptyList(),
            formats = listOf("tv short", "one_shot"),
            year = null,
            sort = "TRENDING_DESC",
            season = null,
            onList = null,
            strings = EnglishStrings,
        )
        assertEquals("[\"TV_SHORT\",\"ONE_SHOT\"]", varsOf(client.requests.single())["format_in"])
    }
}
