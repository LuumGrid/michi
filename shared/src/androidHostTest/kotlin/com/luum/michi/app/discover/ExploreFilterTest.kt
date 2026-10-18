package com.luum.michi.app.discover

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.domain.model.MediaCatalogFilter
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
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
    private fun empty(): NetworkResult<Page<ExploreResult>> =
        NetworkResult.Success(Page(emptyList(), false, 1))

    override suspend fun searchMedia(
        type: MediaType,
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> {
        val call = CatalogCall(query, genres, formats, year, sort, season)
        if (type == MediaType.MANGA) mangaCalls += call else catalogCalls += call
        return empty()
    }

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> = NetworkResult.Success(Page(emptyList(), false, page))

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ) = empty()

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ) = empty()

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ) = empty()
}

private fun varsOf(request: AniListGraphQLRequest): Map<String, String> =
    request.variables?.entries?.associate { (k, v) -> k to v.toString() }.orEmpty()

class ExploreFilterTest {

    // ---- Holder -> repository wiring (caso exacto reportado) ----

    @Test
    fun applyFiltersLoadsOnceAndIgnoresTheUnchangedDraft() = runBlocking {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), stringsProvider = { EnglishStrings })
        holder.submitSearch("naruto")
        val afterSubmit = repo.catalogCalls.size

        // The draft equals what is applied: closing the sheet fires nothing.
        holder.applyFilters(holder.appliedFilters)
        assertEquals(afterSubmit, repo.catalogCalls.size)

        // A changed draft fires exactly one request carrying it.
        holder.applyFilters(
            ExploreFilters(
                genres = listOf("Adventure", "Comedy"),
                formats = listOf("TV"),
                year = 2025,
            ),
        )
        assertEquals(afterSubmit + 1, repo.catalogCalls.size)
        val call = repo.catalogCalls.last()
        assertEquals(listOf("Adventure", "Comedy"), call.genres)
        assertEquals(listOf("TV"), call.formats)
        assertEquals(2025, call.year)
    }

    @Test
    fun clearingFiltersAfterApplyingLoadsOnce() = runBlocking {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), stringsProvider = { EnglishStrings })
        holder.submitSearch("naruto")
        holder.applyFilters(ExploreFilters(genres = listOf("Action")))
        val before = repo.catalogCalls.size

        holder.applyFilters(ExploreFilters.Empty)

        assertEquals(before + 1, repo.catalogCalls.size)
        assertEquals(emptyList(), repo.catalogCalls.last().genres)
    }

    @Test
    fun sortChangeLoadsOnceAndIgnoresTheActiveOne() = runBlocking {
        val repo = FakeExploreRepository()
        val holder = ExploreStateHolder(repo, CoroutineScope(Dispatchers.Unconfined), stringsProvider = { EnglishStrings })
        holder.submitSearch("naruto")
        val before = repo.catalogCalls.size

        holder.updateSort(UserListSort.AVERAGE_SCORE, UserListOrder.ASCENDING)
        assertEquals(before + 1, repo.catalogCalls.size)
        assertEquals("SCORE", repo.catalogCalls.last().sort)

        holder.updateSort(UserListSort.AVERAGE_SCORE, UserListOrder.ASCENDING)
        assertEquals(before + 1, repo.catalogCalls.size, "re-picking the active sort is a no-op")
    }

    // ---- Repository -> variables GraphQL exactas ----

    @Test
    fun catalogVariablesMatchUserCase() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchMedia(
            type = MediaType.ANIME,
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
        assertEquals("\"ANIME\"", vars["type"])
        assertEquals("[\"Adventure\",\"Comedy\"]", vars["genre_in"])
        assertEquals("[\"TV\",\"MOVIE\"]", vars["format_in"])
        assertEquals("2025", vars["seasonYear"])
        assertEquals("[\"TRENDING_DESC\"]", vars["sort"])
        assertEquals(null, vars["season"])
        assertEquals(null, vars["search"])
        assertEquals("MediaCatalog", client.requests.single().operationName)
    }

    @Test
    fun mangaYearUsesFuzzyDateBounds() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchMedia(
            type = MediaType.MANGA,
            query = null,
            genres = emptyList(),
            formats = emptyList(),
            year = 2025,
            sort = "TRENDING_DESC",
            onList = null,
            strings = EnglishStrings,
        )
        val vars = varsOf(client.requests.single())
        assertEquals("\"MANGA\"", vars["type"])
        assertEquals("20241231", vars["startDate_greater"])
        assertEquals("20260101", vars["startDate_lesser"])
        assertEquals(null, vars["seasonYear"])
    }

    @Test
    fun blankFiltersOmitOptionalVariables() = runBlocking {
        val client = FakeGraphQLClient()
        val repo = ExploreRepositoryImpl(client)
        repo.searchMedia(
            type = MediaType.ANIME,
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
        repo.searchMedia(
            type = MediaType.ANIME,
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
