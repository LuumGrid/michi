package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.core.repository.anilist.dto.FuzzyDateDto
import com.luum.michi.app.core.repository.anilist.dto.MediaCharacterConnectionDto
import com.luum.michi.app.core.repository.anilist.dto.MediaCharacterEdgeDto
import com.luum.michi.app.core.repository.anilist.dto.MediaDetailDto
import com.luum.michi.app.core.repository.anilist.dto.MediaRelationEdgeDto
import com.luum.michi.app.core.repository.anilist.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.repository.anilist.dto.MediaStaffEdgeDto
import com.luum.michi.app.core.repository.anilist.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.repository.anilist.dto.MediaVoiceActorDto
import com.luum.michi.app.core.repository.anilist.dto.bestTitle
import com.luum.michi.app.core.repository.anilist.dto.toTitleCase
import com.luum.michi.app.core.domain.model.CalendarDateParts
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.model.calendarPartsToMillis
import com.luum.michi.app.core.domain.model.parseMediaSeason
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.core.domain.util.stripHtml
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.StudioRef
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.core.domain.medialist.MediaListViewerEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.domain.media.model.MediaScoreBucket
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStatsStatus
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStatusBucket
import com.luum.michi.app.mediaDetail.domain.media.model.MediaVoiceActor
import com.luum.michi.app.core.domain.medialist.parseMediaListStatus

internal fun MediaDetailDto.toDomain(strings: LanguageStrings): MediaDetail = MediaDetail(
    id = id,
    type = parseType(type),
    title = title.bestTitle(),
    coverUrl = coverImage?.thumbnailUrl,
    coverFullUrl = coverImage?.bestUrl,
    bannerUrl = bannerImage,
    paletteHex = coverImage?.color,
    format = format?.toTitleCase(),
    status = status?.toTitleCase(),
    episodes = episodes?.takeIf { it > 0 },
    chapters = chapters?.takeIf { it > 0 },
    volumes = volumes?.takeIf { it > 0 },
    duration = duration?.takeIf { it > 0 },
    genres = genres,
    studios = studios?.nodes?.map { StudioRef(it.id, it.name) }.orEmpty(),
    source = source?.toTitleCase(),
    season = formatSeason(season, seasonYear, strings),
    startedLabel = startDate?.format(strings),
    endedLabel = endDate?.format(strings),
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
        .orEmpty()
        .distinctBy { it.mediaId },
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
        coverUrl = node.coverImage?.thumbnailUrl,
        paletteHex = node.coverImage?.color,
        kind = parseRelationKind(relationType),
        format = node.format?.toTitleCase(),
        year = node.seasonYear ?: node.startDate?.year,
        averageScore = node.averageScore?.takeIf { it > 0 },
        favourites = node.favourites?.takeIf { it > 0 },
        viewerStatus = node.mediaListEntry?.status,
    )
}

private fun parseRelationKind(raw: String?): MediaRelationKind = when (raw) {
    "SEQUEL" -> MediaRelationKind.SEQUEL
    "PREQUEL" -> MediaRelationKind.PREQUEL
    "SIDE_STORY" -> MediaRelationKind.SIDE_STORY
    "SPIN_OFF" -> MediaRelationKind.SPIN_OFF
    "PARENT" -> MediaRelationKind.PARENT
    "ADAPTATION" -> MediaRelationKind.ADAPTATION
    "ALTERNATIVE" -> MediaRelationKind.ALTERNATIVE
    "SOURCE" -> MediaRelationKind.SOURCE
    "SUMMARY" -> MediaRelationKind.SUMMARY
    "CHARACTER" -> MediaRelationKind.CHARACTER
    else -> MediaRelationKind.OTHER
}

private fun MediaViewerListEntryDto.toViewerEntry(): MediaListViewerEntry = MediaListViewerEntry(
    id = id,
    status = parseMediaListStatus(status),
    progress = progress ?: 0,
    progressVolumes = progressVolumes,
    score = (score ?: 0.0).toFloat(),
    notes = notes.orEmpty(),
    repeat = repeat ?: 0,
    priority = priority ?: 0,
    isPrivate = isPrivate ?: false,
    hiddenFromStatusLists = hiddenFromStatusLists ?: false,
    startedAtMillis = startedAt?.toMillisOrNull(),
    completedAtMillis = completedAt?.toMillisOrNull(),
)

internal fun FuzzyDateDto.toMillisOrNull(): Long? {
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

private fun formatSeason(season: String?, year: Int?, strings: LanguageStrings): String? {
    if (season == null && year == null) return null
    val seasonLabel = parseMediaSeason(season)?.let { seasonLabel(it, strings) }
    return when {
        seasonLabel != null && year != null -> "$seasonLabel $year"
        seasonLabel != null -> seasonLabel
        else -> year?.toString()
    }
}

private fun seasonLabel(season: MediaSeason, strings: LanguageStrings): String = when (season) {
    MediaSeason.WINTER -> strings.seasonWinterLabel
    MediaSeason.SPRING -> strings.seasonSpringLabel
    MediaSeason.SUMMER -> strings.seasonSummerLabel
    MediaSeason.FALL -> strings.seasonFallLabel
}

private fun FuzzyDateDto.format(strings: LanguageStrings): String? {
    if (year == null && month == null && day == null) return null
    return when {
        year != null && month != null && day != null -> "${strings.monthShortName(month)} $day, $year"
        year != null && month != null -> "${strings.monthShortName(month)} $year"
        year != null -> year.toString()
        month != null && day != null -> "${strings.monthShortName(month)} $day"
        month != null -> strings.monthShortName(month)
        else -> null
    }
}

