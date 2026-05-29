package com.luum.michi.app.studioDetail.data

import com.luum.michi.app.core.anilist.dto.MediaCoverImageDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.studioDetail.presentation.model.StudioDetail
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaItem
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaPage
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaSort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val StudioMediaPerPage = 20

private const val StudioDetailQuery = """
query StudioDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Studio(id: ${'$'}id) {
    id
    name
    isAnimationStudio
    favourites
    isFavourite
    media(isMain: true, page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        type
        format
        title { userPreferred romaji english native }
        coverImage { large color }
        averageScore
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
    media(isMain: true, page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        type
        format
        title { userPreferred romaji english native }
        coverImage { large color }
        averageScore
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
    @SerialName("Studio") val studio: StudioDto? = null,
)

@Serializable
private data class StudioMediaPageResponseDto(
    @SerialName("Studio") val studio: StudioPageOnlyDto? = null,
)

@Serializable
private data class StudioDto(
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
    val pageInfo: StudioPageInfoDto? = null,
    val nodes: List<StudioMediaNodeDto> = emptyList(),
)

@Serializable
private data class StudioPageInfoDto(
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
)

@Serializable
private data class StudioMediaNodeDto(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val startDate: StudioFuzzyYearDto? = null,
    val mediaListEntry: StudioMediaListEntryDto? = null,
)

@Serializable
private data class StudioFuzzyYearDto(
    val year: Int? = null,
)

@Serializable
private data class StudioMediaListEntryDto(
    val status: String? = null,
)

@Serializable
private data class ToggleStudioFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

// ────────────── mapper helpers ──────────────

private fun StudioMediaNodeDto.toItem(): StudioMediaItem = StudioMediaItem(
    mediaId = id,
    title = title?.bestTitle() ?: "",
    coverUrl = coverImage?.thumbnailUrl,
    palette = hexToPalette(coverImage?.color),
    format = format?.toTitleCase(),
    year = startDate?.year,
    averageScore = averageScore?.takeIf { it > 0 },
    viewerStatus = mediaListEntry?.status,
)

private fun MediaTitleDto.bestTitle(): String =
    userPreferred ?: english ?: romaji ?: native ?: ""

private fun String.toTitleCase(): String =
    replace('_', ' ').lowercase().split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun StudioMediaConnectionDto?.toMediaPage(fallbackPage: Int): StudioMediaPage {
    val nodes = this?.nodes.orEmpty()
    return StudioMediaPage(
        items = nodes.map { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class StudioDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : StudioDetailRepository {

    override suspend fun loadDetail(id: Int, sort: StudioMediaSort): NetworkResult<StudioDetail> {
        val request = AniListGraphQLRequest(
            query = StudioDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(StudioMediaPerPage),
                    "sort" to JsonArray(
                        listOf(JsonPrimitive(sort.apiValue))
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
                            isAnimationStudio = studio.isAnimationStudio,
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
        sort: StudioMediaSort,
    ): NetworkResult<StudioMediaPage> {
        val request = AniListGraphQLRequest(
            query = StudioMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(StudioMediaPerPage),
                    "sort" to JsonArray(
                        listOf(JsonPrimitive(sort.apiValue))
                    ),
                ),
            ),
            operationName = "StudioMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StudioMediaPageResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.studio?.media.toMediaPage(fallbackPage = page),
            )
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
