package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.FuzzyDateDto
import com.luum.michi.app.core.anilist.dto.MediaCharacterConnectionDto
import com.luum.michi.app.core.anilist.dto.MediaCharacterEdgeDto
import com.luum.michi.app.core.anilist.dto.MediaDetailDto
import com.luum.michi.app.core.anilist.dto.MediaRelationEdgeDto
import com.luum.michi.app.core.anilist.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.anilist.dto.MediaStaffEdgeDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.anilist.dto.MediaVoiceActorDto
import com.luum.michi.app.core.media.CalendarDateParts
import com.luum.michi.app.core.media.calendarPartsToMillis
import com.luum.michi.app.core.media.millisToCalendarParts
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailType
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailViewerEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.presentation.model.MediaScoreBucket
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffPage
import com.luum.michi.app.mediaDetail.presentation.model.MediaStatsStatus
import com.luum.michi.app.mediaDetail.presentation.model.MediaStatusBucket
import com.luum.michi.app.mediaDetail.presentation.model.MediaVoiceActor
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
    scoreDistribution = stats?.scoreDistribution
        ?.map { MediaScoreBucket(score = it.score, amount = it.amount) }
        ?.sortedBy { it.score }
        .orEmpty(),
    statusDistribution = stats?.statusDistribution
        ?.map { MediaStatusBucket(status = parseStatsStatus(it.status), amount = it.amount) }
        .orEmpty(),
    characters = characters.toCharactersPage(currentPage = 1),
    staff = staff.toStaffPage(currentPage = 1),
)

internal fun MediaCharacterConnectionDto?.toCharactersPage(currentPage: Int): MediaCharactersPage {
    val edges = this?.edges.orEmpty()
    return MediaCharactersPage(
        items = edges.mapIndexedNotNull { index, edge -> edge.toEntry(currentPage, index) },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: currentPage,
    )
}

internal fun MediaStaffConnectionDto?.toStaffPage(currentPage: Int): MediaStaffPage {
    val edges = this?.edges.orEmpty()
    return MediaStaffPage(
        items = edges.mapIndexedNotNull { index, edge -> edge.toEntry(currentPage, index) },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: currentPage,
    )
}

private fun MediaCharacterEdgeDto.toEntry(page: Int, index: Int): MediaCharacterEntry? {
    val node = node ?: return null
    val edgeKey = "p${page}_e${id ?: index}_c${node.id}"
    return MediaCharacterEntry(
        edgeKey = edgeKey,
        characterId = node.id,
        name = node.name?.bestName.orEmpty(),
        imageUrl = node.image?.bestUrl,
        role = parseCharacterRole(role),
        voiceActor = voiceActors.firstOrNull()?.toVoiceActor(),
    )
}

private fun MediaVoiceActorDto.toVoiceActor(): MediaVoiceActor = MediaVoiceActor(
    id = id,
    name = name?.bestName.orEmpty(),
    imageUrl = image?.bestUrl,
    language = languageV2,
)

private fun MediaStaffEdgeDto.toEntry(page: Int, index: Int): MediaStaffEntry? {
    val node = node ?: return null
    val edgeKey = "p${page}_e${id ?: index}_s${node.id}"
    return MediaStaffEntry(
        edgeKey = edgeKey,
        staffId = node.id,
        name = node.name?.bestName.orEmpty(),
        imageUrl = node.image?.bestUrl,
        role = role,
    )
}

private fun parseCharacterRole(raw: String?): MediaCharacterRole = when (raw) {
    "MAIN" -> MediaCharacterRole.MAIN
    "SUPPORTING" -> MediaCharacterRole.SUPPORTING
    "BACKGROUND" -> MediaCharacterRole.BACKGROUND
    else -> MediaCharacterRole.OTHER
}

private fun parseStatsStatus(raw: String?): MediaStatsStatus = when (raw) {
    "CURRENT" -> MediaStatsStatus.CURRENT
    "PLANNING" -> MediaStatsStatus.PLANNING
    "COMPLETED" -> MediaStatsStatus.COMPLETED
    "DROPPED" -> MediaStatsStatus.DROPPED
    "PAUSED" -> MediaStatsStatus.PAUSED
    "REPEATING" -> MediaStatsStatus.REPEATING
    else -> MediaStatsStatus.OTHER
}

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
