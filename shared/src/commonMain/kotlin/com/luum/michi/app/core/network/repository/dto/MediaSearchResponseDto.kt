package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for the `Page.media(search:)` search query.
 */
@Serializable
internal data class MediaSearchResponseDto(
    @SerialName("Page") val page: MediaSearchPageDto? = null,
)

@Serializable
internal data class MediaSearchPageDto(
    val pageInfo: PageInfoDto? = null,
    val media: List<MediaDto> = emptyList(),
)
