package com.luum.michi.app.mediaDetail.repository.character

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.repository.dto.FuzzyDateDto
import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.MediaListStatusDto
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
import com.luum.michi.app.core.util.toHtmlContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.network.repository.personCoverSizes
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.core.network.domain.ExternalLinkCatalogRepository
import com.luum.michi.app.core.network.domain.resolveWithCatalog


private const val CharacterDetailQuery = """
query CharacterDetail(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}voiceLanguage: StaffLanguage) {
  Character(id: ${'$'}id) {
    id
    name { userPreferred native alternative }
    image { large medium }
    description(asHtml: true)
    gender
    age
    bloodType
    dateOfBirth { year month day }
    favourites
    isFavourite
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: [POPULARITY_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        characterRole
        voiceActors(language: ${'$'}voiceLanguage, sort: [RELEVANCE, LANGUAGE]) {
          id
          name { userPreferred }
          image { large medium }
          languageV2
        }
        node {
          id
          type
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

private const val CharacterMediaPageQuery = """
query CharacterMediaPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}voiceLanguage: StaffLanguage) {
  Character(id: ${'$'}id) {
    id
    media(page: ${'$'}page, perPage: ${'$'}perPage, sort: [POPULARITY_DESC]) {
      pageInfo { hasNextPage currentPage }
      edges {
        characterRole
        voiceActors(language: ${'$'}voiceLanguage, sort: [RELEVANCE, LANGUAGE]) {
          id
          name { userPreferred }
          image { large medium }
          languageV2
        }
        node {
          id
          type
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

// ────────────── private DTOs ──────────────

@Serializable
private data class CharacterDetailResponseDto(
    @SerialName("Character") val character: CharacterDetailDto? = null,
)

@Serializable
private data class CharacterMediaPageResponseDto(
    @SerialName("Character") val character: CharacterPageOnlyDto? = null,
)

@Serializable
private data class CharacterDetailDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
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
private data class CharacterMediaConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val edges: List<CharacterMediaEdgeDto> = emptyList(),
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
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
    val languageV2: String? = null,
)

@Serializable
private data class CharacterMediaNodeDto(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val averageScore: Int? = null,
    val startDate: FuzzyDateDto? = null,
    val mediaListEntry: MediaListStatusDto? = null,
)

// ────────────── mapper helpers ──────────────

/** Keep only the canonical roles; the UI localizes the raw value via strings. */
private fun normalizeCharacterRole(raw: String?): String? = when (raw) {
    "MAIN", "SUPPORTING", "BACKGROUND" -> raw
    else -> null
}

private fun CharacterMediaEdgeDto.toItem(): CharacterMediaItem? {
    val node = node ?: return null
    val va = voiceActors.firstOrNull()
    return CharacterMediaItem(
        mediaId = node.id,
        title = node.title?.bestTitle() ?: "",
        cover = node.coverImage?.toCoverSizes(),
        paletteHex = node.coverImage?.color,
        role = normalizeCharacterRole(characterRole),
        format = node.format?.toTitleCase(),
        year = node.startDate?.year,
        averageScore = node.averageScore?.takeIf { it > 0 },
        viewerStatus = node.mediaListEntry?.status,
        voiceActorName = va?.name?.userPreferred,
        voiceActorImage = va?.image?.let { personCoverSizes(it.large, it.medium) },
        voiceActorLanguage = va?.languageV2,
        voiceActorId = va?.id,
    )
}

private fun CharacterMediaConnectionDto?.toMediaPage(fallbackPage: Int): Page<CharacterMediaItem> {
    val edges = this?.edges.orEmpty()
    return Page(
        items = edges.mapNotNull { it.toItem() },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: fallbackPage,
    )
}

// ────────────── repository ──────────────

internal class CharacterDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val externalLinkCatalog: ExternalLinkCatalogRepository,
    private val favouriteRepository: FavouriteRepository,
) : CharacterDetailRepository {

    override suspend fun loadDetail(
        id: Int,
        voiceLanguage: String,
        strings: LanguageStrings,
    ): NetworkResult<CharacterDetail> {
        // Warm the site catalog in its own scope: it overlaps this query and is
        // only awaited if the biography actually carries links.
        externalLinkCatalog.prefetch()
        val request = AniListGraphQLRequest(
            query = CharacterDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(1),
                    "perPage" to JsonPrimitive(RailPageSize),
                    "voiceLanguage" to JsonPrimitive(voiceLanguage),
                ),
            ),
            operationName = "CharacterDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(CharacterDetailResponseDto.serializer(), dataJson)
        }

        return result
            .map { it.character }
            .requireNode("Character", id)
            .map { character ->
                val bio = character.description?.toHtmlContent()
                CharacterDetail(
                    id = character.id,
                    name = character.name?.bestName ?: "",
                    nativeName = character.name?.native?.takeIf { it.isNotBlank() },
                    alternativeNames = character.name?.alternative
                        ?.filter { it.isNotBlank() }.orEmpty(),
                    image = character.image?.let { personCoverSizes(it.large, it.medium) },
                    descriptionPlain = bio?.text.orEmpty(),
                    stats = bio?.stats.orEmpty(),
                    links = bio?.links.orEmpty().resolveWithCatalog(externalLinkCatalog),
                    gender = character.gender?.takeIf { it.isNotBlank() },
                    age = character.age?.takeIf { it.isNotBlank() },
                    bloodType = character.bloodType?.takeIf { it.isNotBlank() },
                    birthday = character.dateOfBirth?.format(strings, includeYear = false),
                    favourites = character.favourites?.takeIf { it > 0 },
                    isFavourite = character.isFavourite,
                    media = character.media.toMediaPage(fallbackPage = 1),
                )
            }
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<Page<CharacterMediaItem>> {
        val request = AniListGraphQLRequest(
            query = CharacterMediaPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(id),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(RailPageSize),
                    "voiceLanguage" to JsonPrimitive(voiceLanguage),
                ),
            ),
            operationName = "CharacterMediaPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(CharacterMediaPageResponseDto.serializer(), dataJson)
        }

        return result
            .map { it.character }
            .requireNode("Character", id)
            .map { it.media.toMediaPage(fallbackPage = page) }
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> =
        favouriteRepository.toggleFavourite(FavouriteTarget.Character(id))
}
