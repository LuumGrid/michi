package com.luum.michi.app.mediaDetail.repository.studio

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.model.MediaGridPage
import com.luum.michi.app.core.network.repository.dto.MediaSearchItemDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.domain.requireNode
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.toMediaGridItem
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.network.domain.GridPageSize

/** The grid node selection, shared by both studio queries (they used to carry a
 *  copy each). It is exactly the catalog node the shared grid mapper reads. */
private const val StudioMediaNodeSelection = """
        id
        format
        title { userPreferred romaji english native }
        coverImage { extraLarge large medium color }
        averageScore
        favourites
        genres
        startDate { year }
        mediaListEntry { id status }
"""

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
      nodes {$StudioMediaNodeSelection}
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
      nodes {$StudioMediaNodeSelection}
    }
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
    val nodes: List<MediaSearchItemDto> = emptyList(),
)

// ────────────── mapper helpers ──────────────

private fun StudioMediaConnectionDto?.toMediaPage(fallbackPage: Int): MediaGridPage {
    val nodes = this?.nodes.orEmpty()
    return MediaGridPage(
        items = nodes.map { it.toMediaGridItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class StudioDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val favouriteRepository: FavouriteRepository,
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

        return result
            .map { it.studio }
            .requireNode("Studio", id)
            .map { studio ->
                StudioDetail(
                    id = studio.id,
                    name = studio.name,
                    isAnimeStudio = studio.isAnimationStudio,
                    favourites = studio.favourites?.takeIf { it > 0 },
                    isFavourite = studio.isFavourite,
                    media = studio.media.toMediaPage(fallbackPage = 1),
                )
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

        return result
            .map { it.studio }
            .requireNode("Studio", id)
            .map { it.media.toMediaPage(fallbackPage = page) }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> =
        favouriteRepository.toggleFavourite(FavouriteTarget.Studio(id))
}
