package com.luum.michi.app.profile

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Shared scripted GraphQL fake for profile tests (single definition on purpose).
 *  [gate] holds every request in flight until completed; [requests] records them
 *  for query-shape assertions. */
internal class FakeProfileGraphQL(
    private val pages: List<JsonElement>,
    private val failureAt: Int = -1,
) : AniListGraphQLClient {
    var calls = 0
    var gate: CompletableDeferred<Unit>? = null
    val requests = mutableListOf<AniListGraphQLRequest>()

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val index = calls++
        requests += request
        gate?.await()
        if (index == failureAt) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(parseData(pages[index]))
    }
}

internal fun profileData(animeCount: Int = 10) = buildJsonObject {
    put("User", buildJsonObject {
        put("statistics", buildJsonObject {
            put("anime", buildJsonObject { put("count", animeCount) })
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

/** One favourites node shaped for [category] (media/person/studio). */
private fun favoriteNode(category: String, id: Int, title: String): JsonObject = buildJsonObject {
    put("id", id)
    when (category) {
        "studios" -> put("name", title)
        "characters", "staff" -> put("name", buildJsonObject { put("userPreferred", title) })
        else -> put("title", buildJsonObject { put("userPreferred", title) })
    }
}

internal fun favouritesPage(category: String, title: String, hasNextPage: Boolean, id: Int = 2) =
    favouritesPageOf(category, hasNextPage, id to title)

/** Multi-node favourites page, for overlap/dedup cases. */
internal fun favouritesPageOf(
    category: String,
    hasNextPage: Boolean,
    vararg nodes: Pair<Int, String>,
) = buildJsonObject {
    put("User", buildJsonObject {
        put("favourites", buildJsonObject {
            put(category, buildJsonObject {
                put("nodes", JsonArray(nodes.map { favoriteNode(category, it.first, it.second) }))
                put("pageInfo", buildJsonObject { put("hasNextPage", hasNextPage) })
            })
        })
    })
}
