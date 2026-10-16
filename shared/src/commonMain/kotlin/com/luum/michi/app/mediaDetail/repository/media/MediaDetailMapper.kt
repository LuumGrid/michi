package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.core.network.repository.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.network.repository.dto.format
import com.luum.michi.app.core.network.repository.dto.toMillisOrNull
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.common.stripHtml
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.mediaDetail.domain.media.model.MediaNextEpisode
import com.luum.michi.app.mediaDetail.domain.media.model.MediaTag
import com.luum.michi.app.mediaDetail.domain.media.model.MediaTrailer
import com.luum.michi.app.mediaDetail.domain.media.model.StudioRef
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaEntryEditorData
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.network.repository.toCoverSizes

internal fun MediaDetailDto.toDomain(strings: LanguageStrings): MediaDetail {
    val studioEdges = studios.toStudioEdges()
    return MediaDetail(
        id = id,
        type = parseType(type),
        title = title.bestTitle(),
        cover = coverImage?.toCoverSizes(),
        bannerUrl = bannerImage,
        paletteHex = coverImage?.color,
        format = format?.toTitleCase(),
        status = parseMediaWorkStatus(status),
        episodes = episodes?.takeIf { it > 0 },
        chapters = chapters?.takeIf { it > 0 },
        volumes = volumes?.takeIf { it > 0 },
        duration = duration?.takeIf { it > 0 },
        genres = genres,
        studios = studioEdges.animationStudios(),
        producers = studioEdges.producers(),
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
        nextEpisode = nextAiringEpisode?.let {
            MediaNextEpisode(
                episode = it.episode,
                airingAtEpochSeconds = it.airingAt,
                timeUntilAiringSeconds = it.timeUntilAiring,
            )
        },
        viewerEntry = mediaListEntry?.toViewerEntry(),
        relations = relations?.edges
            ?.mapNotNull { it.toRelation() }
            .orEmpty()
            .distinctBy { it.mediaId },
        characters = characters.toCharactersPage(currentPage = 1),
        staff = staff.toStaffPage(currentPage = 1),
        titleRomaji = title?.romaji,
        titleNative = title?.native,
        synonyms = synonyms,
        tags = tags.mapNotNull { dto ->
            dto.name?.takeIf { it.isNotBlank() }?.let { name ->
                MediaTag(name = name, rank = dto.rank, isMediaSpoiler = dto.isMediaSpoiler)
            }
        },
        externalLinks = externalLinks.mapNotNull { dto ->
            if (dto.isDisabled == true) return@mapNotNull null
            dto.url?.takeIf { it.isNotBlank() }?.let { url ->
                dto.site?.takeIf { it.isNotBlank() }?.let { site ->
                    ExternalLink(
                        site = site,
                        url = url,
                        type = dto.type,
                        iconUrl = dto.icon,
                        colorHex = dto.color,
                        language = dto.language,
                    )
                }
            }
        },
        trailer = trailer?.let {
            MediaTrailer(videoId = it.id.orEmpty(), site = it.site, thumbnailUrl = it.thumbnail)
        }?.takeIf { it.videoId.isNotBlank() },
    )
}

/**
 * One studio edge of the media's `studios` connection, flattened: AniList sends
 * a single list mixing animation studios and producers, with the main one
 * flagged per edge. Deduped by id (the same node can repeat).
 */
private data class StudioEdge(
    val ref: StudioRef,
    val isMain: Boolean,
    val isAnimationStudio: Boolean,
)

private fun MediaStudioConnectionDto?.toStudioEdges(): List<StudioEdge> =
    this?.edges.orEmpty()
        .mapNotNull { edge ->
            edge.node?.let { StudioEdge(StudioRef(it.id, it.name), edge.isMain, it.isAnimationStudio) }
        }
        .distinctBy { it.ref.id }

/** Animation studios, the main one first (the server's NAME order stays inside each group). */
private fun List<StudioEdge>.animationStudios(): List<StudioRef> =
    filter { it.isAnimationStudio }.sortedByDescending { it.isMain }.map { it.ref }

/** The rest: production companies that are not animation studios. */
private fun List<StudioEdge>.producers(): List<StudioRef> =
    filterNot { it.isAnimationStudio }.map { it.ref }

internal fun MediaCharacterConnectionDto?.toCharactersPage(currentPage: Int): Page<MediaCharacterEntry> {
    val edges = this?.edges.orEmpty()
    return Page(
        items = edges.mapIndexedNotNull { index, edge -> edge.toEntry(currentPage, index) },
        hasNextPage = this?.pageInfo?.hasNextPage == true,
        currentPage = this?.pageInfo?.currentPage ?: currentPage,
    )
}

internal fun MediaStaffConnectionDto?.toStaffPage(currentPage: Int): Page<MediaStaffEntry> {
    val edges = this?.edges.orEmpty()
    return Page(
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
        image = node.image?.toCoverSizes(),
        role = parseCharacterRole(role),
    )
}

private fun MediaStaffEdgeDto.toEntry(page: Int, index: Int): MediaStaffEntry? {
    val node = node ?: return null
    val edgeKey = "p${page}_e${id ?: index}_s${node.id}"
    return MediaStaffEntry(
        edgeKey = edgeKey,
        staffId = node.id,
        name = node.name?.bestName.orEmpty(),
        image = node.image?.toCoverSizes(),
        role = role,
    )
}

private fun parseCharacterRole(raw: String?): MediaCharacterRole = when (raw) {
    "MAIN" -> MediaCharacterRole.MAIN
    "SUPPORTING" -> MediaCharacterRole.SUPPORTING
    "BACKGROUND" -> MediaCharacterRole.BACKGROUND
    else -> MediaCharacterRole.OTHER
}

private fun MediaRelationEdgeDto.toRelation(): MediaDetailRelation? {
    val node = node ?: return null
    return MediaDetailRelation(
        mediaId = node.id,
        title = node.title.bestTitle(),
        cover = node.coverImage?.toCoverSizes(),
        kind = parseRelationKind(relationType),
        format = node.format?.toTitleCase(),
        season = parseMediaSeason(node.season),
        year = node.seasonYear ?: node.startDate?.year,
        workStatus = parseMediaWorkStatus(node.status),
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
    advancedScores = advancedScores.orEmpty(),
    notes = notes.orEmpty(),
    repeat = repeat ?: 0,
    priority = priority ?: 0,
    isPrivate = isPrivate ?: false,
    hiddenFromStatusLists = hiddenFromStatusLists ?: false,
    startedAtMillis = startedAt?.toMillisOrNull(),
    completedAtMillis = completedAt?.toMillisOrNull(),
)

/** The subset the entry editor needs, from its minimal query. */
internal fun MediaDetailDto.toEditorData(): MediaEntryEditorData = MediaEntryEditorData(
    title = title.bestTitle(),
    type = parseType(type),
    episodes = episodes?.takeIf { it > 0 },
    chapters = chapters?.takeIf { it > 0 },
    volumes = volumes?.takeIf { it > 0 },
    isFavourite = isFavourite ?: false,
    viewerEntry = mediaListEntry?.toViewerEntry(),
)

private fun parseType(raw: String?): MediaDetailType = when (raw) {
    "ANIME" -> MediaDetailType.ANIME
    "MANGA" -> MediaDetailType.MANGA
    else -> MediaDetailType.UNKNOWN
}

private fun formatSeason(season: String?, year: Int?, strings: LanguageStrings): String? {
    if (season == null && year == null) return null
    val seasonLabel = parseMediaSeason(season)?.label(strings)
    return when {
        seasonLabel != null && year != null -> "$seasonLabel $year"
        seasonLabel != null -> seasonLabel
        else -> year?.toString()
    }
}

