package com.luum.michi.app.discover

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.MediaCatalogFilter
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.FakeGraphQL
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun genrePageJson(includeCurrentPage: Boolean = true) = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", false)
            if (includeCurrentPage) put("currentPage", 1)
        })
        put("media", JsonArray(listOf(
            buildJsonObject {
                put("id", 7)
                put("type", "ANIME")
                put("title", buildJsonObject { put("userPreferred", "My Hero Academia") })
                put("coverImage", buildJsonObject {
                    put("large", "https://img/7.jpg")
                    put("color", "#123456")
                })
                put("genres", JsonArray(listOf(JsonPrimitive("Action"), JsonPrimitive("Comedy"))))
                put("startDate", buildJsonObject { put("year", 2016) })
            },
        )))
    })
}

class GenreMediaGridMapperTest {

    @Test
    fun mapsTheGridItemFields() = runBlocking {
        val repository = ExploreRepositoryImpl(FakeGraphQL(listOf(genrePageJson())))
        val result = repository.searchMediaGrid(
            filter = MediaCatalogFilter(type = MediaType.ANIME, genres = listOf("Action")),
            page = 1,
            sort = "POPULARITY_DESC",
            hideAdult = true,
        )
        assertIs<NetworkResult.Success<Page<MediaGridItem>>>(result)
        val item = result.value.items.single()

        assertEquals(7, item.mediaId)
        assertEquals("My Hero Academia", item.title)
        assertEquals(2016, item.year)
        assertEquals(listOf("Action", "Comedy"), item.genres)
    }

    @Test
    fun theQueryPinsGenreTypeSortPageAndTheAdultGate() = runBlocking {
        val graphQL = FakeGraphQL(listOf(genrePageJson()))
        ExploreRepositoryImpl(graphQL)
            .searchMediaGrid(
                filter = MediaCatalogFilter(type = MediaType.MANGA, genres = listOf("Action")),
                page = 2,
                sort = "SCORE_DESC",
                hideAdult = true,
            )

        val request = graphQL.requests.single()
        assertTrue(request.query.contains("genre_in"), request.query)
        assertFalse(request.query.contains("mediaListEntry"), "the grid reads no viewer list status")
        val variables = request.variables!!
        assertEquals("[\"Action\"]", variables["genre_in"].toString())
        assertEquals("MANGA", (variables["type"] as JsonPrimitive).content)
        assertEquals("2", (variables["page"] as JsonPrimitive).content)
        assertEquals("[\"SCORE_DESC\"]", variables["sort"].toString())
        assertEquals("false", (variables["isAdult"] as JsonPrimitive).content)
        // A genre sends only its filter: no catalog windows, no popularity floor.
        assertFalse(variables.containsKey("countryOfOrigin_in"))
        assertFalse(variables.containsKey("status"))
        assertFalse(variables.containsKey("popularity_greater"))
        assertFalse(variables.containsKey("format_in"))
        assertFalse(variables.containsKey("startDate_greater"))
        assertFalse(variables.containsKey("seasonYear"))
    }

    @Test
    fun aCatalogSendsItsWindowsFormatsCountriesAndFloor() = runBlocking {
        val graphQL = FakeGraphQL(listOf(genrePageJson()))
        ExploreRepositoryImpl(graphQL)
            .searchMediaGrid(
                filter = MediaCatalogFilter(
                    type = MediaType.MANGA,
                    countries = listOf("CN", "TW"),
                    formats = listOf("MANGA", "ONE_SHOT"),
                    status = "RELEASING",
                    startDateGreater = 20251018,
                    popularityGreater = 5000,
                    season = "FALL",
                    seasonYear = 2026,
                ),
                page = 1,
                sort = "SCORE_DESC",
                hideAdult = false,
            )

        val variables = graphQL.requests.single().variables!!
        assertEquals("[\"MANGA\",\"ONE_SHOT\"]", variables["format_in"].toString())
        assertEquals("[\"CN\",\"TW\"]", variables["countryOfOrigin_in"].toString())
        assertEquals("RELEASING", (variables["status"] as JsonPrimitive).content)
        assertEquals("20251018", (variables["startDate_greater"] as JsonPrimitive).content)
        assertEquals("5000", (variables["popularity_greater"] as JsonPrimitive).content)
        // Season is anime-only: a manga catalog never sends it.
        assertFalse(variables.containsKey("season"))
        assertFalse(variables.containsKey("seasonYear"))
    }

    @Test
    fun anAllowedAdultCatalogOmitsTheGate() = runBlocking {
        val graphQL = FakeGraphQL(listOf(genrePageJson()))
        ExploreRepositoryImpl(graphQL)
            .searchMediaGrid(
                filter = MediaCatalogFilter(type = MediaType.ANIME, genres = listOf("Action")),
                page = 1,
                sort = "POPULARITY_DESC",
                hideAdult = false,
            )

        assertTrue(
            graphQL.requests.single().variables?.containsKey("isAdult") != true,
            "isAdult must be omitted (null = both)",
        )
    }

    @Test
    fun aPageWithoutCurrentPageFallsBackToTheRequestedPage() = runBlocking {
        val repository = ExploreRepositoryImpl(FakeGraphQL(listOf(genrePageJson(includeCurrentPage = false))))
        val result = repository.searchMediaGrid(
            filter = MediaCatalogFilter(type = MediaType.ANIME, genres = listOf("Action")),
            page = 3,
            sort = "POPULARITY_DESC",
            hideAdult = true,
        )
        assertIs<NetworkResult.Success<Page<MediaGridItem>>>(result)
        assertEquals(3, result.value.currentPage)
    }
}
