package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.FuzzyDateDto
import com.luum.michi.app.core.anilist.dto.MediaDetailDto
import com.luum.michi.app.core.anilist.dto.MediaRelationEdgeDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.media.CalendarDateParts
import com.luum.michi.app.core.media.calendarPartsToMillis
import com.luum.michi.app.core.media.millisToCalendarParts
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailType
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailViewerEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.presentation.model.parseMediaListStatus

internal fun MediaDetailDto.toDomain(): MediaDetail = MediaDetail(
    id = id,
    type = parseType(type),
    title = title.bestTitle(),
    coverUrl = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium,
    bannerUrl = bannerImage,
    palette = hexToPalette(coverImage?.color),
    format = format?.toTitleCase(),
    status = status?.toTitleCase(),
    episodes = episodes?.takeIf { it > 0 },
    chapters = chapters?.takeIf { it > 0 },
    volumes = volumes?.takeIf { it > 0 },
    duration = duration?.takeIf { it > 0 },
    genres = genres,
    studios = studios?.nodes?.map { it.name }.orEmpty(),
    source = source?.toTitleCase(),
    season = formatSeason(season, seasonYear),
    startedLabel = startDate?.format(),
    endedLabel = endDate?.format(),
    averageScore = averageScore?.takeIf { it > 0 },
    meanScore = meanScore?.takeIf { it > 0 },
    popularity = popularity?.takeIf { it > 0 },
    favourites = favourites?.takeIf { it > 0 },
    descriptionPlain = description?.stripHtml().orEmpty(),
    isAdult = isAdult ?: false,
    isFavourite = isFavourite ?: false,
    viewerEntry = mediaListEntry?.toViewerEntry(),
    relations = relations?.edges
        ?.mapNotNull { it.toRelation() }
        .orEmpty(),
)

private fun MediaRelationEdgeDto.toRelation(): MediaDetailRelation? {
    val node = node ?: return null
    return MediaDetailRelation(
        mediaId = node.id,
        title = node.title.bestTitle(),
        coverUrl = node.coverImage?.extraLarge ?: node.coverImage?.large ?: node.coverImage?.medium,
        palette = hexToPalette(node.coverImage?.color),
        kind = parseRelationKind(relationType),
        format = node.format?.toTitleCase(),
    )
}

private fun parseRelationKind(raw: String?): MediaRelationKind = when (raw) {
    "SEQUEL" -> MediaRelationKind.SEQUEL
    "PREQUEL" -> MediaRelationKind.PREQUEL
    "SIDE_STORY" -> MediaRelationKind.SIDE_STORY
    "SPIN_OFF" -> MediaRelationKind.SPIN_OFF
    "PARENT" -> MediaRelationKind.PARENT
    "ADAPTATION" -> MediaRelationKind.ADAPTATION
    else -> MediaRelationKind.OTHER
}

private fun MediaViewerListEntryDto.toViewerEntry(): MediaDetailViewerEntry = MediaDetailViewerEntry(
    id = id,
    status = parseMediaListStatus(status),
    progress = progress,
    progressVolumes = progressVolumes,
    score = score.toFloat(),
    notes = notes.orEmpty(),
    repeat = repeat,
    priority = priority,
    isPrivate = isPrivate,
    hiddenFromStatusLists = hiddenFromStatusLists,
    startedAtMillis = startedAt?.toMillisOrNull(),
    completedAtMillis = completedAt?.toMillisOrNull(),
)

private fun FuzzyDateDto.toMillisOrNull(): Long? {
    val y = year ?: return null
    val m = month ?: return null
    val d = day ?: return null
    return calendarPartsToMillis(CalendarDateParts(y, m, d))
}

private fun parseType(raw: String?): MediaDetailType = when (raw) {
    "ANIME" -> MediaDetailType.ANIME
    "MANGA" -> MediaDetailType.MANGA
    else -> MediaDetailType.UNKNOWN
}

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun String.toTitleCase(): String = this
    .replace('_', ' ')
    .lowercase()
    .split(' ')
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private fun formatSeason(season: String?, year: Int?): String? {
    if (season == null && year == null) return null
    val seasonLabel = season?.toTitleCase()
    return when {
        seasonLabel != null && year != null -> "$seasonLabel $year"
        seasonLabel != null -> seasonLabel
        else -> year?.toString()
    }
}

private fun FuzzyDateDto.format(): String? {
    if (year == null && month == null && day == null) return null
    return when {
        year != null && month != null && day != null -> "${monthName(month)} $day, $year"
        year != null && month != null -> "${monthName(month)} $year"
        year != null -> year.toString()
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

private val HtmlTagRegex = Regex("<[^>]+>")
private val HtmlEntities = mapOf(
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&nbsp;" to " ",
    "&mdash;" to "—",
    "&ndash;" to "–",
    "&hellip;" to "…",
)
private val NumericEntityRegex = Regex("&#(\\d+);")

private fun String.stripHtml(): String {
    val withBreaks = replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    var result = HtmlTagRegex.replace(withBreaks, "")
    HtmlEntities.forEach { (entity, char) -> result = result.replace(entity, char) }
    result = NumericEntityRegex.replace(result) { match ->
        match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
    }
    return result
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
