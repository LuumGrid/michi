package com.luum.michi.app.characterDetail.data

import com.luum.michi.app.characterDetail.presentation.model.CharacterDetail
import com.luum.michi.app.characterDetail.presentation.model.CharacterMediaItem
import com.luum.michi.app.characterDetail.presentation.model.CharacterMediaPage
import com.luum.michi.app.characterDetail.presentation.model.CharacterMediaSort
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val CharacterMediaPerPage = 20

private const val CharacterDetailQuery = """
query CharacterDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Character(id: ${'$'}id) {
    id
    name { userPreferred native alternative alternativeSpoiler }
    image { large }
    description(asHtml: true)
    gender
    age
    bloodType
    dateOfBirth { year month day }
    favourites
    isFavourite
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      edges {
        characterRole
        voiceActors(sort: [RELEVANCE, LANGUAGE]) {
          id
          name { userPreferred }
          image { large }
          languageV2
        }
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

private const val CharacterMediaPageQuery = """
query CharacterMediaPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
  Character(id: ${'$'}id) {
    id
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: ${'$'}sort) {
      pageInfo { hasNextPage currentPage }
      edges {
        characterRole
        voiceActors(sort: [RELEVANCE, LANGUAGE]) {
          id
          name { userPreferred }
          image { large }
          languageV2
        }
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

private const val ToggleCharacterFavouriteMutation = """
mutation ToggleCharacterFavourite(${'$'}id: Int!) {
  ToggleFavourite(characterId: ${'$'}id) {
    characters { nodes { id } }
  }
}
"""

// ────────────── private DTOs ──────────────

@Serializable
private data class CharacterDetailResponseDto(
    @SerialName("Character") val character: CharacterDto? = null,
)

@Serializable
private data class CharacterMediaPageResponseDto(
    @SerialName("Character") val character: CharacterPageOnlyDto? = null,
)

@Serializable
private data class CharacterDto(
    val id: Int,
    val name: CharacterNameDto? = null,
    val image: CharacterImageDto? = null,
    val description: String? = null,
    val gender: String? = null,
    val age: String? = null,
    val bloodType: String? = null,
    val dateOfBirth: FuzzyDateDto? = null,
    val favourites: Int? = null,
    val isFavourite: Boolean = false,
    val media: CharacterMediaConnectionDto? = null,
)

@Serializable
private data class CharacterPageOnlyDto(
    val id: Int,
    val media: CharacterMediaConnectionDto? = null,
)

@Serializable
private data class CharacterNameDto(
    val userPreferred: String? = null,
    val native: String? = null,
    val alternative: List<String> = emptyList(),
    val alternativeSpoiler: List<String> = emptyList(),
)

@Serializable
private data class CharacterImageDto(
    val large: String? = null,
)

@Serializable
private data class CharacterMediaConnectionDto(
    val pageInfo: CharacterPageInfoDto? = null,
    val edges: List<CharacterMediaEdgeDto> = emptyList(),
)

@Serializable
private data class CharacterPageInfoDto(
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
)

@Serializable
private data class CharacterMediaEdgeDto(
    val characterRole: String? = null,
    val voiceActors: List<VoiceActorDto> = emptyList(),
    val node: CharacterMediaNodeDto? = null,
)

@Serializable
private data class VoiceActorDto(
    val id: Int,
    val name: VoiceActorNameDto? = null,
    val image: CharacterImageDto? = null,
    val languageV2: String? = null,
)

@Serializable
private data class VoiceActorNameDto(
    val userPreferred: String? = null,
)

@Serializable
private data class CharacterMediaNodeDto(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val startDate: CharacterFuzzyYearDto? = null,
    val mediaListEntry: CharacterMediaListEntryDto? = null,
)

@Serializable
private data class CharacterFuzzyYearDto(
    val year: Int? = null,
)

@Serializable
private data class CharacterMediaListEntryDto(
    val status: String? = null,
)

@Serializable
private data class ToggleCharacterFavouriteResponseDto(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
)

// ────────────── mapper helpers ──────────────

private fun MediaTitleDto.bestTitle(): String =
    userPreferred ?: english ?: romaji ?: native ?: ""

private fun String.toTitleCase(): String =
    replace('_', ' ').lowercase().split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

/** Keep only the canonical roles; the UI localizes the raw value via strings. */
private fun normalizeCharacterRole(raw: String?): String? = when (raw) {
    "MAIN", "SUPPORTING", "BACKGROUND" -> raw
    else -> null
}

private fun FuzzyDateDto.formatBirthday(): String? {
    if (month == null && day == null) return null
    return when {
        month != null && day != null -> "${monthName(month)} $day"
        month != null -> monthName(month)
        else -> null
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}

private fun CharacterMediaEdgeDto.toItem(): CharacterMediaItem? {
    val node = node ?: return null
    val va = voiceActors.firstOrNull()
    return CharacterMediaItem(
        mediaId = node.id,
        title = node.title?.bestTitle() ?: "",
        coverUrl = node.coverImage?.thumbnailUrl,
        palette = hexToPalette(node.coverImage?.color),
        role = normalizeCharacterRole(characterRole),
        format = node.format?.toTitleCase(),
        year = node.startDate?.year,
        averageScore = node.averageScore?.takeIf { it > 0 },
        viewerStatus = node.mediaListEntry?.status,
        voiceActorName = va?.name?.userPreferred,
        voiceActorImageUrl = va?.image?.large,
        voiceActorLanguage = va?.languageV2,
        voiceActorId = va?.id,
    )
}

private fun CharacterMediaConnectionDto?.toMediaPage(fallbackPage: Int): CharacterMediaPage {
    val edges = this?.edges.orEmpty()
    return CharacterMediaPage(
        items = edges.mapNotNull { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class CharacterDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : CharacterDetailRepository {

    override suspend fun loadDetail(id: Int, sort: CharacterMediaSort): NetworkResult<CharacterDetail> {
        val request = AniListGraphQLRequest(
            query = CharacterDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(CharacterMediaPerPage),
                    "sort" to JsonArray(listOf(JsonPrimitive(sort.apiValue))),
                ),
            ),
            operationName = "CharacterDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(CharacterDetailResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val character = result.value.character
                if (character == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Character not found for id=$id")),
                    )
                } else {
                    NetworkResult.Success(
                        CharacterDetail(
                            id = character.id,
                            name = character.name?.userPreferred ?: "",
                            nativeName = character.name?.native?.takeIf { it.isNotBlank() },
                            alternativeNames = character.name?.alternative
                                ?.filter { it.isNotBlank() }.orEmpty(),
                            alternativeSpoilerNames = character.name?.alternativeSpoiler
                                ?.filter { it.isNotBlank() }.orEmpty(),
                            imageUrl = character.image?.large,
                            descriptionPlain = character.description?.stripHtml().orEmpty(),
                            gender = character.gender?.takeIf { it.isNotBlank() },
                            age = character.age?.takeIf { it.isNotBlank() },
                            bloodType = character.bloodType?.takeIf { it.isNotBlank() },
                            birthday = character.dateOfBirth?.formatBirthday(),
                            favourites = character.favourites?.takeIf { it > 0 },
                            isFavourite = character.isFavourite,
                            media = character.media.toMediaPage(fallbackPage = 1),
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
        sort: CharacterMediaSort,
    ): NetworkResult<CharacterMediaPage> {
        val request = AniListGraphQLRequest(
            query = CharacterMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(CharacterMediaPerPage),
                    "sort" to JsonArray(listOf(JsonPrimitive(sort.apiValue))),
                ),
            ),
            operationName = "CharacterMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(CharacterMediaPageResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.character?.media.toMediaPage(fallbackPage = page),
            )
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        val request = AniListGraphQLRequest(
            query = ToggleCharacterFavouriteMutation,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(id)),
            ),
            operationName = "ToggleCharacterFavourite",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ToggleCharacterFavouriteResponseDto.serializer(), dataJson)
        }

        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Failure -> result
        }
    }
}
