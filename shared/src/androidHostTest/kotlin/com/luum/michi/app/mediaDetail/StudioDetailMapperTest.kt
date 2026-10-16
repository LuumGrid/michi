package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.favourite.repository.FavouriteRepositoryImpl
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.repository.studio.StudioDetailRepositoryImpl
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

private fun studioDetailJson() = buildJsonObject {
    put("Studio", buildJsonObject {
        put("id", 9)
        put("name", "Bones")
        put("isAnimationStudio", true)
        put("favourites", 1234)
        put("isFavourite", true)
        put("media", buildJsonObject {
            put("pageInfo", buildJsonObject {
                put("hasNextPage", false)
                put("currentPage", 1)
            })
            put("nodes", JsonArray(listOf(
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
    })
}

class StudioDetailMapperTest {

    @Test
    fun mapsFavouritesAndGenres() = runBlocking {
        val graphQL = FakeGraphQL(listOf(studioDetailJson()))
        val repository = StudioDetailRepositoryImpl(graphQL, FavouriteRepositoryImpl(graphQL))
        val result = repository.loadDetail(9, "POPULARITY_DESC")
        assertIs<NetworkResult.Success<StudioDetail>>(result)
        val detail = result.value

        assertEquals(1234, detail.favourites)
        val item = detail.media.items.single()
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
    fun studioQueriesRequestFavouritesAndGenres() = runBlocking {
        val graphQL = FakeGraphQL(listOf(studioDetailJson()))
        StudioDetailRepositoryImpl(graphQL, FavouriteRepositoryImpl(graphQL)).loadDetail(9, "POPULARITY_DESC")

        val query = graphQL.requests.single().query
        assertTrue(query.contains("genres"), query)
        assertTrue(query.contains("favourites"), query)
    }
}
