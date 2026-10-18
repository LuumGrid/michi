package com.luum.michi.app.profile

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Scripted [FavouriteRepository] fake: records targets, can hold requests at a
 * [gate], answer a failure, and bumps the shared [revision] like the real one.
 * [bump] simulates a toggle made from another surface (a detail heart or the
 * editor) for the staleness tests.
 */
internal class FakeFavouriteRepository(
    var result: NetworkResult<Unit> = NetworkResult.Success(Unit),
) : FavouriteRepository {
    private val _revision = MutableStateFlow(0)
    override val revision: StateFlow<Int> = _revision.asStateFlow()

    val targets = mutableListOf<FavouriteTarget>()
    var gate: CompletableDeferred<Unit>? = null

    /** The number of toggle calls (all targets). */
    val calls: Int get() = targets.size

    override suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit> {
        targets += target
        gate?.await()
        if (result is NetworkResult.Success) _revision.update { it + 1 }
        return result
    }
}


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

internal fun profileData(animeCount: Int = 10) = buildJsonObject {    put("User", buildJsonObject {
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

// ── Full UserProfile response with several favourites buckets ───────────────

/** The UserProfile query's shape with the given favourites, for removal tests. */
internal fun profileJson(
    anime: List<Pair<Int, String>> = emptyList(),
    manga: List<Pair<Int, String>> = emptyList(),
    studios: List<Pair<Int, String>> = emptyList(),
): JsonObject = buildJsonObject {
    put("User", buildJsonObject {
        put("statistics", buildJsonObject {
            put("anime", buildJsonObject { put("count", anime.size) })
            put("manga", buildJsonObject { put("count", manga.size) })
        })
        put("favourites", buildJsonObject {
            put("anime", favouritesBucket(anime.map { mediaFavoriteJson(it.first, it.second) }))
            put("manga", favouritesBucket(manga.map { mediaFavoriteJson(it.first, it.second) }))
            put("characters", favouritesBucket(emptyList()))
            put("staff", favouritesBucket(emptyList()))
            put("studios", favouritesBucket(studios.map { studioFavoriteJson(it.first, it.second) }))
        })
    })
    put("followers", buildJsonObject { put("pageInfo", buildJsonObject { put("total", 0) }) })
    put("following", buildJsonObject { put("pageInfo", buildJsonObject { put("total", 0) }) })
}

private fun favouritesBucket(nodes: List<JsonObject>): JsonObject = buildJsonObject {
    put("nodes", JsonArray(nodes))
}

private fun mediaFavoriteJson(id: Int, name: String): JsonObject = buildJsonObject {
    put("id", id)
    put("title", buildJsonObject { put("userPreferred", name) })
}

private fun studioFavoriteJson(id: Int, name: String): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
}
