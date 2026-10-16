package com.luum.michi.app.mediaDetail.repository.genre

import com.luum.michi.app.core.model.MediaType
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.GridPageSize
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.repository.dto.MediaSearchItemDto
import com.luum.michi.app.core.network.repository.dto.MediaSearchResponseDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.mediaDetail.domain.genre.GenreDetailRepository
import com.luum.michi.app.mediaDetail.domain.model.MediaGridItem
import com.luum.michi.app.mediaDetail.domain.model.MediaGridPage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// `genre_in` takes a list; a single `$name` string coerces to a one-item list.
// The node shape is the studio grid's, so both pages read the same card.
private const val GenreMediaPageQuery = """
query GenreMediaPage(${'$'}name: String, ${'$'}type: MediaType, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort], ${'$'}isAdult: Boolean) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage currentPage }
    media(genre_in: [${'$'}name], type: ${'$'}type, sort: ${'$'}sort, isAdult: ${'$'}isAdult) {
      id
      format
      title { userPreferred romaji english native }
      coverImage { extraLarge large medium color }
      averageScore
      favourites
      genres
      startDate { year }
      mediaListEntry { id status }
    }
  }
}
"""

private fun MediaSearchItemDto.toGridItem(): MediaGridItem = MediaGridItem(
    mediaId = id,
    title = title?.bestTitle() ?: "",
    cover = coverImage?.toCoverSizes(),
    paletteHex = coverImage?.color,
    format = format?.toTitleCase(),
    year = startDate?.year,
    averageScore = averageScore?.takeIf { it > 0 },
    viewerStatus = mediaListEntry?.status,
    favourites = favourites?.takeIf { it > 0 },
    genres = genres.orEmpty().filter { it.isNotBlank() },
)

internal class GenreDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : GenreDetailRepository {

    override suspend fun loadPage(
        name: String,
        type: MediaType,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<MediaGridPage> {
        val variables = buildMap<String, JsonElement> {
            put("name", JsonPrimitive(name))
            put("type", JsonPrimitive(type.name))
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(GridPageSize))
            put("sort", JsonArray(listOf(JsonPrimitive(sort))))
            // Omit (null) when adult is allowed: the server then returns both.
            // Never send true — that would filter to adult ONLY.
            if (hideAdult) {
                put("isAdult", JsonPrimitive(false))
            }
        }
        val request = AniListGraphQLRequest(
            query = GenreMediaPageQuery,
            variables = JsonObject(variables),
            operationName = "GenreMediaPage",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            MediaGridPage(
                items = response.page?.media.orEmpty().map { it.toGridItem() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
                currentPage = response.page?.pageInfo?.currentPage ?: page,
            )
        }
    }
}
