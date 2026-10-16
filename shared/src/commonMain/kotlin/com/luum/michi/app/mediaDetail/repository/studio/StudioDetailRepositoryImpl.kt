package com.luum.michi.app.mediaDetail.repository.studio

import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.FuzzyDateDto
import com.luum.michi.app.core.network.repository.dto.MediaListStatusDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.model.MediaGridItem
import com.luum.michi.app.mediaDetail.domain.model.MediaGridPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.core.network.domain.GridPageSize


private const val StudioDetailQuery = """
query StudioDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Studio(id: ${'$'}id) {
    id
    name
    isAnimationStudio
    favourites
    isFavourite
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        format
        title { userPreferred romaji english native }
        coverImage { extraLarge large medium color }
        averageScore
        favourites
        genres
        startDate { year }
        mediaListEntry { status }
      }
    }
  }
}
"""

private const val StudioMediaPageQuery = """
query StudioMediaPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Studio(id: ${'$'}id) {
    id
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        format
        title { userPreferred romaji english native }
        coverImage { extraLarge large medium color }
        averageScore
        favourites
        genres
        startDate { year }
        mediaListEntry { status }
      }
    }
  }
}
"""

private const val ToggleStudioFavouriteMutation = """
mutation ToggleStudioFavourite(${'$'}id: Int!) {
  ToggleFavourite(studioId: ${'$'}id) {
    studios { nodes { id } }
  }
}
"""

// ────────────── private DTOs ──────────────

@Serializable
private data class StudioDetailResponseDto(
    @SerialName("Studio") val studio: StudioDetailDto? = null,
)

@Serializable
private data class StudioMediaPageResponseDto(
    @SerialName("Studio") val studio: StudioPageOnlyDto? = null,
)

@Serializable
private data class StudioDetailDto(
    val id: Int,
    val name: String,
    val isAnimationStudio: Boolean = false,
    val favourites: Int? = null,
    val isFavourite: Boolean = false,
    val media: StudioMediaConnectionDto? = null,
)

@Serializable
private data class StudioPageOnlyDto(
    val id: Int,
    val media: StudioMediaConnectionDto? = null,
)

@Serializable
private data class StudioMediaConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val nodes: List<StudioMediaNodeDto> = emptyList(),
)

@Serializable
private data class StudioMediaNodeDto(
    val id: Int,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val startDate: FuzzyDateDto? = null,
    val mediaListEntry: MediaListStatusDto? = null,
)

@Serializable
private data class ToggleStudioFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

// ────────────── mapper helpers ──────────────

private fun StudioMediaNodeDto.toItem(): MediaGridItem = MediaGridItem(
    mediaId = id,
    title = title?.bestTitle() ?: "",
    cover = coverImage?.toCoverSizes(),
    paletteHex = coverImage?.color,
    format = format?.toTitleCase(),
    year = startDate?.year,
    averageScore = averageScore?.takeIf { it > 0 },
    viewerStatus = mediaListEntry?.status,
    favourites = favourites?.takeIf { it > 0 },
    genres = genres.filter { it.isNotBlank() },
)

private fun StudioMediaConnectionDto?.toMediaPage(fallbackPage: Int): MediaGridPage {
    val nodes = this?.nodes.orEmpty()
    return MediaGridPage(
        items = nodes.map { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class StudioDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : StudioDetailRepository {

    override suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail> {
        val request = AniListGraphQLRequest(
            query = StudioDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(GridPageSize),
                    "sort" to JsonArray(
                        listOf(JsonPrimitive(sort))
                    ),
                ),
            ),
            operationName = "StudioDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StudioDetailResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val studio = result.value.studio
                if (studio == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Studio not found for id=$id")),
                    )
                } else {
                    NetworkResult.Success(
                        StudioDetail(
                            id = studio.id,
                            name = studio.name,
                            isAnimeStudio = studio.isAnimationStudio,
                            favourites = studio.favourites?.takeIf { it > 0 },
                            isFavourite = studio.isFavourite,
                            media = studio.media.toMediaPage(fallbackPage = 1),
                        ),
                    )
                }
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        sort: String,
    ): NetworkResult<MediaGridPage> {
        val request = AniListGraphQLRequest(
            query = StudioMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(GridPageSize),
                    "sort" to JsonArray(
                        listOf(JsonPrimitive(sort))
                    ),
                ),
            ),
            operationName = "StudioMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StudioMediaPageResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val studio = result.value.studio
                if (studio == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Studio not found for id=$id")),
                    )
                } else {
                    NetworkResult.Success(studio.media.toMediaPage(fallbackPage = page))
                }
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        val request = AniListGraphQLRequest(
            query = ToggleStudioFavouriteMutation,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(id)),
            ),
            operationName = "ToggleStudioFavourite",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ToggleStudioFavouriteResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Failure -> result
        }
    }
}
