package com.luum.michi.app.search.data

import com.luum.michi.app.core.anilist.dto.MediaSearchItemDto
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.search.presentation.model.SearchResult

internal fun MediaSearchItemDto.toSearchResult(): SearchResult {
    val titleText = title?.userPreferred ?: title?.english ?: title?.romaji ?: title?.native ?: ""
    return SearchResult(
        id = id,
        title = titleText,
        meta = buildMeta(),
        coverUrl = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium,
        palette = hexToPalette(coverImage?.color),
        averageScore = averageScore?.takeIf { it > 0 },
    )
}

private fun MediaSearchItemDto.buildMeta(): String {
    val parts = mutableListOf<String>()
    format?.toTitleCase()?.let(parts::add)
    when (type) {
        "ANIME" -> episodes?.takeIf { it > 0 }?.let { parts.add("$it ep") }
        "MANGA" -> chapters?.takeIf { it > 0 }?.let { parts.add("$it ch") }
    }
    (seasonYear ?: startDate?.year)?.let { parts.add(it.toString()) }
    return parts.joinToString(" · ")
}

private fun String.toTitleCase(): String = this
    .replace('_', ' ')
    .lowercase()
    .split(' ')
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
