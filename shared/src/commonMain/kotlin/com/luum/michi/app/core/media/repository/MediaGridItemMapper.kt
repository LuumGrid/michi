package com.luum.michi.app.core.media.repository

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.toTitleCase
import com.luum.michi.app.core.media.repository.dto.MediaDto

/**
 * The one catalog-node → grid-cell mapper. Studio works and genre works both
 * read their cards from a `Page.media` node, so the mapping lives here instead
 * of in either feature (they used to carry a copy each).
 */
internal fun MediaDto.toMediaGridItem(): MediaGridItem = MediaGridItem(
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
