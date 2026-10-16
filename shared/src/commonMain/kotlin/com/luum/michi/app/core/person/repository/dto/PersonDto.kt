package com.luum.michi.app.core.person.repository.dto

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import kotlinx.serialization.Serializable

/**
 * AniList `Character` and `Staff` share the same name/image shape. We unify
 * them into a single set of DTOs to avoid duplication.
 */
@Serializable
internal data class PersonNameDto(
    val full: String? = null,
    val first: String? = null,
    val last: String? = null,
    val userPreferred: String? = null,
    val native: String? = null,
    val alternative: List<String> = emptyList(),
) {
    val bestName: String
        get() = userPreferred ?: full ?: listOfNotNull(first, last).joinToString(" ").ifBlank { null } ?: ""
}

@Serializable
internal data class PersonImageDto(
    val large: String? = null,
    val medium: String? = null,
)

@Serializable
internal data class CharacterDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
)

@Serializable
internal data class StaffDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
)

@Serializable
internal data class StudioLatestMediaNodeDto(
    val status: String? = null,
    val coverImage: MediaCoverImageDto? = null,
)

@Serializable
internal data class StudioLatestMediaConnectionDto(
    val nodes: List<StudioLatestMediaNodeDto>? = null,
)

@Serializable
internal data class StudioDto(
    val id: Int,
    val name: String,
    val media: StudioLatestMediaConnectionDto? = null,
) {
    val latestCoverImage: MediaCoverImageDto?
        get() = media?.nodes
            ?.firstOrNull { it.status == "FINISHED" || it.status == "RELEASING" }
            ?.coverImage
}
