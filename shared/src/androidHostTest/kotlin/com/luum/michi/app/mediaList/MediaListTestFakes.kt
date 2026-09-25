package com.luum.michi.app.mediaList

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Shared scripted GraphQL fake for mediaList tests (single definition on purpose). */
internal class FakeGraphQL(
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

private fun titleJson(romaji: String, english: String? = null, native: String? = null) =
    buildJsonObject {
        put("romaji", romaji)
        if (english != null) put("english", english)
        if (native != null) put("native", native)
    }

internal fun animeMediaJson(
    id: Int,
    format: String = "TV",
    mediaStatus: String? = "RELEASING",
    romaji: String = "Romaji Title",
    english: String? = "English Title",
    native: String? = null,
    episodes: Int? = 12,
    season: String? = "WINTER",
    seasonYear: Int? = 2024,
    genres: List<String> = emptyList(),
): JsonObject = buildJsonObject {
    put("id", id)
    put("title", titleJson(romaji, english, native))
    put("format", format)
    if (mediaStatus != null) put("status", mediaStatus)
    if (episodes != null) put("episodes", episodes)
    put("coverImage", buildJsonObject { put("thumbnailUrl", "https://img/$id.jpg") })
    if (season != null) put("season", season)
    if (seasonYear != null) put("seasonYear", seasonYear)
    put("genres", JsonArray(genres.map { JsonPrimitive(it) }))
}

internal fun mangaMediaJson(
    id: Int,
    format: String = "MANGA",
    mediaStatus: String? = "FINISHED",
    romaji: String = "Romaji Manga",
    chapters: Int? = 100,
    volumes: Int? = 10,
): JsonObject = buildJsonObject {
    put("id", id)
    put("title", titleJson(romaji))
    put("format", format)
    if (mediaStatus != null) put("status", mediaStatus)
    if (chapters != null) put("chapters", chapters)
    if (volumes != null) put("volumes", volumes)
    put("coverImage", buildJsonObject { put("thumbnailUrl", "https://img/$id.jpg") })
}

internal fun listEntryJson(
    id: Int,
    status: String,
    progress: Int,
    score: Double = 0.0,
    progressVolumes: Int? = null,
    media: JsonObject,
): JsonObject = buildJsonObject {
    put("id", id)
    put("status", status)
    put("progress", progress)
    put("score", score)
    if (progressVolumes != null) put("progressVolumes", progressVolumes)
    put("media", media)
}

/** Single-group collection: entries carry their own list status. */
internal fun singleGroupCollection(vararg entries: JsonObject): JsonElement = buildJsonObject {
    put("MediaListCollection", buildJsonObject {
        put("lists", JsonArray(listOf(
            buildJsonObject {
                put("name", "All")
                put("status", "CURRENT")
                put("isCustomList", false)
                put("entries", JsonArray(entries.toList()))
            },
        )))
    })
}

internal val emptyUpdateAck: JsonElement = buildJsonObject {
    put("SaveMediaListEntry", buildJsonObject { put("id", 1) })
}

/** In-memory SortPersistence: per-scope slots, like the store impl. */
internal class MemorySortPersistence : SortPersistence {
    private val slots = mutableMapOf<SortScope, Pair<String, String>>()
    val cleared = mutableListOf<SortScope>()

    override fun saveSort(scope: SortScope, sort: String, order: String) {
        slots[scope] = sort to order
    }

    override fun loadSort(scope: SortScope): Pair<String, String>? = slots[scope]

    override fun clearSort(scope: SortScope) {
        slots.remove(scope)
        cleared += scope
    }
}
