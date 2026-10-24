package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.core.media.domain.parseMediaFormat
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.core.media.repository.toCoverSizes

internal fun MediaDto.toExploreResult(strings: LanguageStrings): ExploreResult {
    val titleText = title.bestTitle()
    return ExploreResult(
        id = id,
        title = titleText,
        meta = buildMeta(strings),
        cover = coverImage?.toCoverSizes(),
        paletteHex = coverImage?.color,
    )
}

private fun MediaDto.buildMeta(strings: LanguageStrings): String {
    val parts = mutableListOf<String>()
    parts.add(parseMediaFormat(format).label(unknownFallback = if (type == "MANGA") strings.mangaLabel else strings.animeLabel))
    when (type) {
        "ANIME" -> episodes?.takeIf { it > 0 }?.let { parts.add(strings.exploreEpisodeCountLabel(it)) }
        "MANGA" -> chapters?.takeIf { it > 0 }?.let { parts.add(strings.exploreChapterCountLabel(it)) }
    }
    (seasonYear ?: startDate?.year)?.let { parts.add(it.toString()) }
    return parts.joinToString(" · ")
}
