package com.luum.michi.app.account

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Shared scripted GraphQL fake for account tests (single definition on purpose). */
internal class FakeAccountGraphQL(
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

internal fun accountData() = buildJsonObject {
    put("User", buildJsonObject {
        put("statistics", buildJsonObject {
            put("anime", buildJsonObject { put("count", 10) })
            put("manga", buildJsonObject { put("count", 5) })
        })
        put("favourites", buildJsonObject {
            put("anime", buildJsonObject {
                put("nodes", JsonArray(listOf(buildJsonObject {
                    put("id", 1)
                    put("title", buildJsonObject { put("userPreferred", "A") })
                })))
            })
        })
    })
    put("followers", buildJsonObject {
        put("pageInfo", buildJsonObject { put("total", 7) })
    })
    put("following", buildJsonObject {
        put("pageInfo", buildJsonObject { put("total", 9) })
    })
}

internal fun favouritesPage(category: String, title: String, hasNextPage: Boolean, id: Int = 2) =
    buildJsonObject {
        put("User", buildJsonObject {
            put("favourites", buildJsonObject {
                put(category, buildJsonObject {
                    put("nodes", JsonArray(listOf(buildJsonObject {
                        put("id", id)
                        if (category == "studios") {
                            put("name", title)
                        } else if (category == "characters" || category == "staff") {
                            put("name", buildJsonObject { put("userPreferred", title) })
                        } else {
                            put("title", buildJsonObject { put("userPreferred", title) })
                        }
                    })))
                    put("pageInfo", buildJsonObject { put("hasNextPage", hasNextPage) })
                })
            })
        })
    }
