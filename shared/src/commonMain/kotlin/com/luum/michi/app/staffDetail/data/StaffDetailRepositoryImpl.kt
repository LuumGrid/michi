package com.luum.michi.app.staffDetail.data

import com.luum.michi.app.core.anilist.dto.FuzzyDateDto
import com.luum.michi.app.core.anilist.dto.MediaCoverImageDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.core.util.stripHtml
import com.luum.michi.app.staffDetail.presentation.model.StaffCharacterItem
import com.luum.michi.app.staffDetail.presentation.model.StaffCharacterPage
import com.luum.michi.app.staffDetail.presentation.model.StaffDetail
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaItem
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaPage
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaSort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val StaffPerPage = 20

private const val StaffDetailQuery = """
query StaffDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}charPage: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Staff(id: ${'$'}id) {
    id
    name { userPreferred native alternative }
    image { large }
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
    staffMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      edges {
        staffRole
        node {
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
    characterMedia(page: ${'$'}charPage, perPage: ${'$'}perPage, sort: [START_DATE_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        node { id title { userPreferred } }
        characters { id name { userPreferred } image { large } }
      }
    }
  }
}
"""

private const val StaffMediaPageQuery = """
query StaffMediaPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Staff(id: ${'$'}id) {
    id
    staffMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      edges {
        staffRole
        node {
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
}
"""

private const val StaffCharacterPageQuery = """
query StaffCharacterPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Staff(id: ${'$'}id) {
    id
    characterMedia(page: ${'$'}page, perPage: ${'$'}perPage, sort: [START_DATE_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        node { id title { userPreferred } }
        characters { id name { userPreferred } image { large } }
      }
    }
  }
}
"""

private const val ToggleStaffFavouriteMutation = """
mutation ToggleStaffFavourite(${'$'}id: Int!) {
  ToggleFavourite(staffId: ${'$'}id) {
    staff { nodes { id } }
  }
}
"""

// ────────────── private DTOs ──────────────

@Serializable
private data class StaffDetailResponseDto(
    @SerialName("Staff") val staff: StaffDto? = null,
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
private data class StaffDto(
    val id: Int,
    val name: StaffNameDto? = null,
    val image: StaffImageDto? = null,
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
private data class StaffNameDto(
    val userPreferred: String? = null,
    val native: String? = null,
    val alternative: List<String> = emptyList(),
)

@Serializable
private data class StaffImageDto(
    val large: String? = null,
)

@Serializable
private data class StaffMediaConnectionDto(
    val pageInfo: StaffPageInfoDto? = null,
    val edges: List<StaffMediaEdgeDto> = emptyList(),
)

@Serializable
private data class StaffCharacterMediaConnectionDto(
    val pageInfo: StaffPageInfoDto? = null,
    val edges: List<StaffCharacterMediaEdgeDto> = emptyList(),
)

@Serializable
private data class StaffPageInfoDto(
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
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
    val type: String? = null,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val startDate: StaffFuzzyYearDto? = null,
    val mediaListEntry: StaffMediaListEntryDto? = null,
)

@Serializable
private data class StaffCharacterMediaNodeDto(
    val id: Int,
    val title: MediaTitleDto? = null,
)

@Serializable
private data class StaffCharacterNodeDto(
    val id: Int,
    val name: StaffCharacterNameDto? = null,
    val image: StaffImageDto? = null,
)

@Serializable
private data class StaffCharacterNameDto(
    val userPreferred: String? = null,
)

@Serializable
private data class StaffFuzzyYearDto(
    val year: Int? = null,
)

@Serializable
private data class StaffMediaListEntryDto(
    val status: String? = null,
)

@Serializable
private data class ToggleStaffFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

// ────────────── mapper helpers ──────────────

private fun MediaTitleDto.bestTitle(): String =
    userPreferred ?: english ?: romaji ?: native ?: ""

private fun String.toTitleCase(): String =
    replace('_', ' ').lowercase().split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun FuzzyDateDto.formatDate(): String? {
    if (month == null && day == null && year == null) return null
    return when {
        month != null && day != null && year != null -> "${monthName(month)} $day, $year"
        month != null && day != null -> "${monthName(month)} $day"
        month != null && year != null -> "${monthName(month)} $year"
        year != null -> "$year"
        month != null -> monthName(month)
        else -> null
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}

private fun StaffMediaEdgeDto.toItem(): StaffMediaItem? {
    val node = node ?: return null
    return StaffMediaItem(
        mediaId = node.id,
        title = node.title?.bestTitle() ?: "",
        coverUrl = node.coverImage?.thumbnailUrl,
        palette = hexToPalette(node.coverImage?.color),
        staffRole = staffRole?.takeIf { it.isNotBlank() },
        format = node.format?.toTitleCase(),
        year = node.startDate?.year,
        averageScore = node.averageScore?.takeIf { it > 0 },
    )
}

private fun StaffCharacterMediaEdgeDto.toItems(): List<StaffCharacterItem> {
    val mediaTitle = node?.title?.bestTitle()
    return characters.map { char ->
        StaffCharacterItem(
            characterId = char.id,
            name = char.name?.userPreferred ?: "",
            imageUrl = char.image?.large,
            mediaTitle = mediaTitle,
        )
    }
}

private fun StaffMediaConnectionDto?.toMediaPage(fallbackPage: Int): StaffMediaPage {
    return StaffMediaPage(
        items = this?.edges.orEmpty().mapNotNull { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

private fun StaffCharacterMediaConnectionDto?.toCharacterPage(fallbackPage: Int): StaffCharacterPage {
    return StaffCharacterPage(
        items = this?.edges.orEmpty().flatMap { it.toItems() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class StaffDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : StaffDetailRepository {

    override suspend fun loadDetail(id: Int, sort: StaffMediaSort): NetworkResult<StaffDetail> {
        val request = AniListGraphQLRequest(
            query = StaffDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "charPage" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(StaffPerPage),
                    "sort" to JsonArray(listOf(JsonPrimitive(sort.apiValue))),
                ),
            ),
            operationName = "StaffDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffDetailResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val staff = result.value.staff
                if (staff == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Staff not found for id=$id")),
                    )
                } else {
                    val yearsActive = staff.yearsActive
                    NetworkResult.Success(
                        StaffDetail(
                            id = staff.id,
                            name = staff.name?.userPreferred ?: "",
                            nativeName = staff.name?.native?.takeIf { it.isNotBlank() },
                            alternativeNames = staff.name?.alternative
                                ?.filter { it.isNotBlank() }.orEmpty(),
                            imageUrl = staff.image?.large,
                            descriptionPlain = staff.description?.stripHtml().orEmpty(),
                            gender = staff.gender?.takeIf { it.isNotBlank() },
                            age = staff.age?.takeIf { it.isNotBlank() },
                            birthday = staff.dateOfBirth?.formatDate(),
                            death = staff.dateOfDeath?.formatDate(),
                            yearsActiveStart = yearsActive.getOrNull(0)?.takeIf { it > 0 },
                            yearsActiveEnd = yearsActive.getOrNull(1),
                            homeTown = staff.homeTown?.takeIf { it.isNotBlank() },
                            occupations = staff.primaryOccupations
                                .filter { it.isNotBlank() }
                                .joinToString(", ").takeIf { it.isNotBlank() },
                            bloodType = staff.bloodType?.takeIf { it.isNotBlank() },
                            favourites = staff.favourites?.takeIf { it > 0 },
                            isFavourite = staff.isFavourite,
                            media = staff.staffMedia.toMediaPage(fallbackPage = 1),
                            characters = staff.characterMedia.toCharacterPage(fallbackPage = 1),
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
        sort: StaffMediaSort,
    ): NetworkResult<StaffMediaPage> {
        val request = AniListGraphQLRequest(
            query = StaffMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(StaffPerPage),
                    "sort" to JsonArray(listOf(JsonPrimitive(sort.apiValue))),
                ),
            ),
            operationName = "StaffMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffMediaPageResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.staff?.staffMedia.toMediaPage(fallbackPage = page),
            )
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadCharacterPage(id: Int, page: Int): NetworkResult<StaffCharacterPage> {
        val request = AniListGraphQLRequest(
            query = StaffCharacterPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(StaffPerPage),
                ),
            ),
            operationName = "StaffCharacterPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffCharacterPageResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.staff?.characterMedia.toCharacterPage(fallbackPage = page),
            )
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        val request = AniListGraphQLRequest(
            query = ToggleStaffFavouriteMutation,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(id)),
            ),
            operationName = "ToggleStaffFavourite",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ToggleStaffFavouriteResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Failure -> result
        }
    }
}
