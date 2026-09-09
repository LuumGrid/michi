package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.network.repository.dto.MediaSearchItemDto
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.model.ExploreResult

internal fun MediaSearchItemDto.toExploreResult(strings: LanguageStrings): ExploreResult {
    val titleText = title?.userPreferred ?: title?.english ?: title?.romaji ?: title?.native ?: ""
    return ExploreResult(
        id = id,
        title = titleText,
        meta = buildMeta(strings),
        coverUrl = coverImage?.thumbnailUrl,
        paletteHex = coverImage?.color,
        averageScore = averageScore?.takeIf { it > 0 },
        favourites = favourites,
        genres = genres.orEmpty(),
        isUserFavorited = isFavourite == true,
        isUserRanked = mediaListEntry != null && (mediaListEntry.score ?: 0.0) > 0.0,
    )
}

private fun MediaSearchItemDto.buildMeta(strings: LanguageStrings): String {
    val parts = mutableListOf<String>()
    parts.add(parseMediaFormat(format).label(unknownFallback = if (type == "MANGA") "Manga" else "Anime"))
    when (type) {
        "ANIME" -> episodes?.takeIf { it > 0 }?.let { parts.add(strings.exploreEpisodeCountLabel(it)) }
        "MANGA" -> chapters?.takeIf { it > 0 }?.let { parts.add(strings.exploreChapterCountLabel(it)) }
    }
    (seasonYear ?: startDate?.year)?.let { parts.add(it.toString()) }
    return parts.joinToString(" · ")
}
