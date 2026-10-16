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

/** One mutation for every favourite target; only the matching variable is sent. */
private const val ToggleFavouriteMutation = """
mutation ToggleFavourite(
  ${'$'}animeId: Int,
  ${'$'}mangaId: Int,
  ${'$'}characterId: Int,
  ${'$'}staffId: Int,
  ${'$'}studioId: Int
) {
  ToggleFavourite(
    animeId: ${'$'}animeId,
    mangaId: ${'$'}mangaId,
    characterId: ${'$'}characterId,
    staffId: ${'$'}staffId,
    studioId: ${'$'}studioId
  ) {
    anime { nodes { id } }
    manga { nodes { id } }
    characters { nodes { id } }
    staff { nodes { id } }
    studios { nodes { id } }
  }
}
"""

@Serializable
private data class ToggleFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

internal class FavouriteRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : FavouriteRepository {

    override suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit> {
        val variableName = when (target) {
            is FavouriteTarget.Anime -> "animeId"
            is FavouriteTarget.Manga -> "mangaId"
            is FavouriteTarget.Character -> "characterId"
            is FavouriteTarget.Staff -> "staffId"
            is FavouriteTarget.Studio -> "studioId"
        }
        val request = AniListGraphQLRequest(
            query = ToggleFavouriteMutation,
            variables = JsonObject(mapOf(variableName to JsonPrimitive(target.id))),
            operationName = "ToggleFavourite",
        )
        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ToggleFavouriteResponseDto.serializer(), dataJson)
        }.map { }
    }
}
