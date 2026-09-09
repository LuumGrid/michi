package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaSort
import com.luum.michi.app.mediaDetail.repository.staff.StaffDetailRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun staffData(yearsActive: List<Int>, charName: JsonObject, mediaTitle: JsonObject): JsonElement =
    buildJsonObject {
        put("Staff", buildJsonObject {
            put("id", 1)
            put("yearsActive", JsonArray(yearsActive.map { JsonPrimitive(it) }))
            put("characterMedia", buildJsonObject {
                put("edges", JsonArray(listOf(buildJsonObject {
                    put("node", buildJsonObject {
                        put("id", 10)
                        put("title", mediaTitle)
                    })
                    put("characters", JsonArray(listOf(buildJsonObject {
                        put("id", 20)
                        put("name", charName)
                        put("image", buildJsonObject { put("large", "img") })
                    })))
                })))
            })
        })
    }

private fun fakeClient(data: JsonElement) = object : AniListGraphQLClient {
    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> = NetworkResult.Success(parseData(data))
}

private fun loadDetail(data: JsonElement): StaffDetail {
    val result = runBlocking {
        StaffDetailRepositoryImpl(fakeClient(data))
            .loadDetail(1, StaffMediaSort.POPULARITY, EnglishStrings)
    }
    assertTrue(result is NetworkResult.Success, "expected Success but was $result")
    return result.value
}

private fun nameOf(userPreferred: String?, native: String?): JsonObject = buildJsonObject {
    if (userPreferred != null) put("userPreferred", userPreferred)
    if (native != null) put("native", native)
}

private fun titleOf(userPreferred: String?, native: String?): JsonObject = buildJsonObject {
    if (userPreferred != null) put("userPreferred", userPreferred)
    if (native != null) put("native", native)
}

class StaffDetailMinorsTest {

    @Test
    fun characterNameFallsBackToNative() {
        val detail = loadDetail(
            staffData(
                yearsActive = listOf(2010),
                charName = nameOf(userPreferred = null, native = "名前"),
                mediaTitle = titleOf(userPreferred = "Title", native = null),
            ),
        )
        assertEquals("名前", detail.characters.items.single().name)
    }

    @Test
    fun mediaTitleFallsBackThroughChain() {
        val detail = loadDetail(
            staffData(
                yearsActive = listOf(2010),
                charName = nameOf(userPreferred = "Name", native = null),
                mediaTitle = titleOf(userPreferred = null, native = "ナルト"),
            ),
        )
        assertEquals("ナルト", detail.characters.items.single().mediaTitle)
    }

    @Test
    fun yearsActiveZeroEndBecomesNull() {
        val detail = loadDetail(
            staffData(
                yearsActive = listOf(2010, 0),
                charName = nameOf(userPreferred = "Name", native = null),
                mediaTitle = titleOf(userPreferred = "Title", native = null),
            ),
        )
        assertEquals(2010, detail.yearsActiveStart)
        assertNull(detail.yearsActiveEnd)
    }

    @Test
    fun yearsActivePresentEndIsKept() {
        val detail = loadDetail(
            staffData(
                yearsActive = listOf(2005, 2020),
                charName = nameOf(userPreferred = "Name", native = null),
                mediaTitle = titleOf(userPreferred = "Title", native = null),
            ),
        )
        assertEquals(2005, detail.yearsActiveStart)
        assertEquals(2020, detail.yearsActiveEnd)
    }
}
