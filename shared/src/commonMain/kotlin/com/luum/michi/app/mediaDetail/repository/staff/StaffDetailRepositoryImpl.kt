package com.luum.michi.app.mediaDetail.repository.staff

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.repository.dto.FuzzyDateDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.mediaDetail.repository.MediaListStatusDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto
import com.luum.michi.app.core.network.repository.dto.PersonNameDto
import com.luum.michi.app.core.network.repository.dto.format
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.domain.requireNode
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.common.toHtmlContent
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.network.repository.personCoverSizes
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.mediaDetail.domain.common.ExternalLinkCatalogRepository
import com.luum.michi.app.mediaDetail.domain.common.resolveWithCatalog


private const val StaffDetailQuery = """
query StaffDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}charPage: Int!, ${'$'}perPage: Int!) {
  Staff(id: ${'$'}id) {
    id
    name { userPreferred native alternative }
    image { large medium }
    description(asHtml: true)
    primaryOccupations
    gender
    age
    yearsActive
    homeTown
    bloodType
    dateOfBirth { year month day }
    dateOfDeath { year month day }
    favourites
    isFavourite
    staffMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: [POPULARITY_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        staffRole
        node {
          id
          format
          title { userPreferred romaji english native }
          coverImage { extraLarge large medium color }
          averageScore
          startDate { year }
          mediaListEntry { status }
        }
      }
    }
    characterMedia(page: ${'$'}charPage, perPage: ${'$'}perPage, sort: [START_DATE_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        node { id title { userPreferred romaji english native } }
        characters { id name { userPreferred native } image { large medium } }
      }
    }
  }
}
"""

private const val StaffMediaPageQuery = """
query StaffMediaPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Staff(id: ${'$'}id) {
    id
    staffMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: [POPULARITY_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        staffRole
        node {
          id
          format
          title { userPreferred romaji english native }
          coverImage { extraLarge large medium color }
          averageScore
          startDate { year }
          mediaListEntry { status }
        }
      }
    }
  }
}
"""

private const val StaffCharacterPageQuery = """
query StaffCharacterPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Staff(id: ${'$'}id) {
    id
    characterMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: [START_DATE_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        node { id title { userPreferred romaji english native } }
        characters { id name { userPreferred native } image { large medium } }
      }
    }
  }
}
"""

// ────────────── private DTOs ──────────────

@Serializable
private data class StaffDetailResponseDto(
    @SerialName("Staff") val staff: StaffDetailDto? = null,
)

@Serializable
private data class StaffMediaPageResponseDto(
    @SerialName("Staff") val staff: StaffMediaOnlyDto? = null,
)

@Serializable
private data class StaffCharacterPageResponseDto(
    @SerialName("Staff") val staff: StaffCharacterOnlyDto? = null,
)

@Serializable
private data class StaffDetailDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
    val description: String? = null,
    val primaryOccupations: List<String> = emptyList(),
    val gender: String? = null,
    val age: String? = null,
    val yearsActive: List<Int> = emptyList(),
    val homeTown: String? = null,
    val bloodType: String? = null,
    val dateOfBirth: FuzzyDateDto? = null,
    val dateOfDeath: FuzzyDateDto? = null,
    val favourites: Int? = null,
    val isFavourite: Boolean = false,
    val staffMedia: StaffMediaConnectionDto? = null,
    val characterMedia: StaffCharacterMediaConnectionDto? = null,
)

@Serializable
private data class StaffMediaOnlyDto(
    val id: Int,
    val staffMedia: StaffMediaConnectionDto? = null,
)

@Serializable
private data class StaffCharacterOnlyDto(
    val id: Int,
    val characterMedia: StaffCharacterMediaConnectionDto? = null,
)

@Serializable
private data class StaffMediaConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val edges: List<StaffMediaEdgeDto> = emptyList(),
)

@Serializable
private data class StaffCharacterMediaConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val edges: List<StaffCharacterMediaEdgeDto> = emptyList(),
)

@Serializable
private data class StaffMediaEdgeDto(
    val staffRole: String? = null,
    val node: StaffMediaNodeDto? = null,
)

@Serializable
private data class StaffCharacterMediaEdgeDto(
    val node: StaffCharacterMediaNodeDto? = null,
    val characters: List<StaffCharacterNodeDto> = emptyList(),
)

@Serializable
private data class StaffMediaNodeDto(
    val id: Int,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val startDate: FuzzyDateDto? = null,
    val mediaListEntry: MediaListStatusDto? = null,
)

@Serializable
private data class StaffCharacterMediaNodeDto(
    val id: Int,
    val title: MediaTitleDto? = null,
)

@Serializable
private data class StaffCharacterNodeDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
)

// ────────────── mapper helpers ──────────────

private fun StaffMediaEdgeDto.toItem(): StaffMediaItem? {
    val node = node ?: return null
    return StaffMediaItem(
        mediaId = node.id,
        title = node.title?.bestTitle() ?: "",
        cover = node.coverImage?.toCoverSizes(),
        paletteHex = node.coverImage?.color,
        staffRole = staffRole?.takeIf { it.isNotBlank() },
        format = node.format?.toTitleCase(),
        year = node.startDate?.year,
        averageScore = node.averageScore?.takeIf { it > 0 },
    )
}

private fun StaffCharacterMediaEdgeDto.toItems(): List<StaffCharacterItem> {
    val mediaTitle = node?.title?.bestTitle()
    val mediaId = node?.id
    return characters.map { char ->
        StaffCharacterItem(
            characterId = char.id,
            name = char.name?.bestName ?: "",
            image = char.image?.let { personCoverSizes(it.large, it.medium) },
            mediaTitle = mediaTitle,
            mediaId = mediaId,
        )
    }
}

private fun StaffMediaConnectionDto?.toMediaPage(fallbackPage: Int): Page<StaffMediaItem> {
    return Page(
        items = this?.edges.orEmpty().mapNotNull { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

private fun StaffCharacterMediaConnectionDto?.toCharacterPage(fallbackPage: Int): Page<StaffCharacterItem> {
    return Page(
        items = this?.edges.orEmpty().flatMap { it.toItems() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class StaffDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val externalLinkCatalog: ExternalLinkCatalogRepository,
    private val favouriteRepository: FavouriteRepository,
) : StaffDetailRepository {

    override suspend fun loadDetail(id: Int, strings: LanguageStrings): NetworkResult<StaffDetail> {
        // Warm the site catalog in its own scope: it overlaps this query and is
        // only awaited if the biography actually carries links.
        externalLinkCatalog.prefetch()
        val request = AniListGraphQLRequest(
            query = StaffDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "charPage" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(RailPageSize),
                ),
            ),
            operationName = "StaffDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffDetailResponseDto.serializer(), dataJson)
        }

        return result
            .map { it.staff }
            .requireNode("Staff", id)
            .map { staff ->
                val yearsActive = staff.yearsActive
                val bio = staff.description?.toHtmlContent()
                StaffDetail(
                    id = staff.id,
                    name = staff.name?.bestName ?: "",
                    nativeName = staff.name?.native?.takeIf { it.isNotBlank() },
                    alternativeNames = staff.name?.alternative
                        ?.filter { it.isNotBlank() }.orEmpty(),
                    image = staff.image?.let { personCoverSizes(it.large, it.medium) },
                    descriptionPlain = bio?.text.orEmpty(),
                    stats = bio?.stats.orEmpty(),
                    links = bio?.links.orEmpty().resolveWithCatalog(externalLinkCatalog),
                    gender = staff.gender?.takeIf { it.isNotBlank() },
                    age = staff.age?.takeIf { it.isNotBlank() },
                    birthday = staff.dateOfBirth?.format(strings),
                    death = staff.dateOfDeath?.format(strings),
                    yearsActiveStart = yearsActive.getOrNull(0)?.takeIf { it > 0 },
                    yearsActiveEnd = yearsActive.getOrNull(1)?.takeIf { it > 0 },
                    homeTown = staff.homeTown?.takeIf { it.isNotBlank() },
                    occupations = staff.primaryOccupations
                        .filter { it.isNotBlank() }
                        .joinToString(", ").takeIf { it.isNotBlank() },
                    bloodType = staff.bloodType?.takeIf { it.isNotBlank() },
                    favourites = staff.favourites?.takeIf { it > 0 },
                    isFavourite = staff.isFavourite,
                    media = staff.staffMedia.toMediaPage(fallbackPage = 1),
                    characters = staff.characterMedia.toCharacterPage(fallbackPage = 1),
                )
            }
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
    ): NetworkResult<Page<StaffMediaItem>> {
        val request = AniListGraphQLRequest(
            query = StaffMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(RailPageSize),
                ),
            ),
            operationName = "StaffMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffMediaPageResponseDto.serializer(), dataJson)
        }

        return result
            .map { it.staff }
            .requireNode("Staff", id)
            .map { it.staffMedia.toMediaPage(fallbackPage = page) }
    }

    override suspend fun loadCharacterPage(id: Int, page: Int): NetworkResult<Page<StaffCharacterItem>> {
        val request = AniListGraphQLRequest(
            query = StaffCharacterPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(RailPageSize),
                ),
            ),
            operationName = "StaffCharacterPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffCharacterPageResponseDto.serializer(), dataJson)
        }

        return result
            .map { it.staff }
            .requireNode("Staff", id)
            .map { it.characterMedia.toCharacterPage(fallbackPage = page) }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> =
        favouriteRepository.toggleFavourite(FavouriteTarget.Staff(id))
}
