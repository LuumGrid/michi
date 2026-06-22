package com.luum.michi.app.core.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AniList `Viewer` JSON shape. Mapped to the domain [Viewer] via [toDomain].
 *
 * Sample response:
 * ```
 * {
 *   "Viewer": {
 *     "id": 12345,
 *     "name": "psyxho",
 *     "avatar": { "large": "https://..." },
 *     "bannerImage": "https://...",
 *     "about": "...",
 *     "createdAt": 1700000000,
 *     "donatorTier": 0,
 *     "moderatorRoles": null
 *   }
 * }
 * ```
 */
@Serializable
internal data class ViewerResponseDto(
    @SerialName("Viewer")
    val viewer: ViewerDto,
)

@Serializable
internal data class ViewerDto(
    val id: Int,
    val name: String,
    val avatar: AvatarDto? = null,
    val bannerImage: String? = null,
    val about: String? = null,
    val createdAt: Long? = null,
    val donatorTier: Int = 0,
    val moderatorRoles: List<String>? = null,
    val options: ViewerOptionsDto? = null,
    val mediaListOptions: ViewerMediaListOptionsDto? = null,
)

@Serializable
internal data class AvatarDto(
    val large: String? = null,
    val medium: String? = null,
)

@Serializable
internal data class ViewerOptionsDto(
    val titleLanguage: String? = null,
    val staffNameLanguage: String? = null,
    val displayAdultContent: Boolean = false,
)

@Serializable
internal data class ViewerMediaListOptionsDto(
    val scoreFormat: String? = null,
)

internal fun ViewerDto.toDomain(): Viewer = Viewer(
    id = id,
    name = name,
    avatarUrl = avatar?.large ?: avatar?.medium,
    bannerUrl = bannerImage,
    about = about,
    createdAtEpochSeconds = createdAt,
    isDonator = donatorTier > 0,
    isModerator = !moderatorRoles.isNullOrEmpty(),
    titleLanguage = options?.titleLanguage,
    staffNameLanguage = options?.staffNameLanguage,
    displayAdultContent = options?.displayAdultContent ?: false,
    scoreFormat = mediaListOptions?.scoreFormat,
)
