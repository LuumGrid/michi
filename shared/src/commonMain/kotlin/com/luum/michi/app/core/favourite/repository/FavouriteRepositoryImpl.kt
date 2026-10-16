package com.luum.michi.app.core.favourite.repository

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.repository.AniListJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
private data class ToggleFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

internal class FavouriteRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : FavouriteRepository {

    /**
     * One mutation for every target, built so only the target's argument and
     * selection list are sent (the five-variable/five-list version asked for
     * everything on every toggle).
     */
    override suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit> {
        val argument = when (target) {
            is FavouriteTarget.Anime -> "animeId"
            is FavouriteTarget.Manga -> "mangaId"
            is FavouriteTarget.Character -> "characterId"
            is FavouriteTarget.Staff -> "staffId"
            is FavouriteTarget.Studio -> "studioId"
        }
        val selection = when (target) {
            is FavouriteTarget.Anime -> "anime"
            is FavouriteTarget.Manga -> "manga"
            is FavouriteTarget.Character -> "characters"
            is FavouriteTarget.Staff -> "staff"
            is FavouriteTarget.Studio -> "studios"
        }
        val query = """
mutation ToggleFavourite(${'$'}id: Int) {
  ToggleFavourite($argument: ${'$'}id) {
    $selection { nodes { id } }
  }
}
"""
        val request = AniListGraphQLRequest(
            query = query,
            variables = JsonObject(mapOf("id" to JsonPrimitive(target.id))),
            operationName = "ToggleFavourite",
        )
        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ToggleFavouriteResponseDto.serializer(), dataJson)
        }.map { }
    }
}
