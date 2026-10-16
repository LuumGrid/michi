package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.model.MediaType
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.model.MediaGridPage
import com.luum.michi.app.mediaDetail.repository.genre.GenreDetailRepositoryImpl
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun genrePageJson() = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", false)
            put("currentPage", 1)
        })
        put("media", JsonArray(listOf(
            buildJsonObject {
                put("id", 7)
                put("type", "ANIME")
                put("format", "TV")
                put("title", buildJsonObject { put("userPreferred", "My Hero Academia") })
                put("coverImage", buildJsonObject {
                    put("large", "https://img/7.jpg")
                    put("color", "#123456")
                })
                put("averageScore", 81)
                put("favourites", 500)
                put("genres", JsonArray(listOf(JsonPrimitive("Action"), JsonPrimitive("Comedy"))))
                put("startDate", buildJsonObject { put("year", 2016) })
                put("mediaListEntry", buildJsonObject {
                    put("id", 3)
                    put("status", "CURRENT")
                })
            },
        )))
    })
}

class GenreDetailMapperTest {

    @Test
    fun mapsTheGridItemFields() = runBlocking {
        val repository = GenreDetailRepositoryImpl(FakeGraphQL(listOf(genrePageJson())))
        val result = repository.loadPage("Action", MediaType.ANIME, 1, "POPULARITY_DESC", hideAdult = true)
        assertIs<NetworkResult.Success<MediaGridPage>>(result)
        val item = result.value.items.single()

        assertEquals(7, item.mediaId)
        assertEquals("My Hero Academia", item.title)
        assertEquals("TV", item.format)
        assertEquals(2016, item.year)
        assertEquals(81, item.averageScore)
        assertEquals(500, item.favourites)
        assertEquals(listOf("Action", "Comedy"), item.genres)
        assertEquals("CURRENT", item.viewerStatus)
    }

    @Test
    fun theQueryPinsGenreTypeSortPageAndTheAdultGate() = runBlocking {
        val graphQL = FakeGraphQL(listOf(genrePageJson()))
        GenreDetailRepositoryImpl(graphQL)
            .loadPage("Action", MediaType.MANGA, 2, "SCORE_DESC", hideAdult = true)

        val request = graphQL.requests.single()
        assertTrue(request.query.contains("genre_in"), request.query)
        val variables = request.variables!!
        assertEquals("Action", (variables["name"] as JsonPrimitive).content)
        assertEquals("MANGA", (variables["type"] as JsonPrimitive).content)
        assertEquals("2", (variables["page"] as JsonPrimitive).content)
        assertEquals("[\"SCORE_DESC\"]", variables["sort"].toString())
        assertEquals("false", (variables["isAdult"] as JsonPrimitive).content)
    }

    @Test
    fun anAllowedAdultCatalogOmitsTheGate() = runBlocking {
        val graphQL = FakeGraphQL(listOf(genrePageJson()))
        GenreDetailRepositoryImpl(graphQL)
            .loadPage("Action", MediaType.ANIME, 1, "POPULARITY_DESC", hideAdult = false)

        assertTrue(
            graphQL.requests.single().variables?.containsKey("isAdult") != true,
            "isAdult must be omitted (null = both)",
        )
    }
}
