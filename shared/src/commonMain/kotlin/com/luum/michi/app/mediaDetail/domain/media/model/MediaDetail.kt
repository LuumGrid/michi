package com.luum.michi.app.mediaDetail.domain.media.model

import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.medialist.domain.label
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaWorkStatus

internal enum class MediaDetailType { ANIME, MANGA, UNKNOWN }

internal data class StudioRef(val id: Int, val name: String)

internal data class MediaDetail(
    val id: Int,
    val type: MediaDetailType,
    val title: String,
    val coverUrl: String?,
    val coverFullUrl: String? = null,
    val bannerUrl: String?,
    val paletteHex: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val duration: Int?,
    val genres: List<String>,
    val studios: List<StudioRef>,
    val source: String?,
    val season: String?,
    val startedLabel: String?,
    val endedLabel: String?,
    val averageScore: Int?,
    val meanScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val descriptionPlain: String,
    val isAdult: Boolean,
    val isFavourite: Boolean,
    val viewerEntry: MediaListViewerEntry?,
    val relations: List<MediaDetailRelation>,
    val characters: MediaCharactersPage,
    val staff: MediaStaffPage,
    val titleRomaji: String?,
    val titleNative: String?,
    val synonyms: List<String>,
    val rankings: List<MediaRank>,
    val tags: List<MediaTag>,
    val externalLinks: List<MediaExternalLink>,
    val trailer: MediaTrailer?,
    val nextEpisode: MediaNextEpisode? = null,
)

/** Next airing episode of a releasing show (null for finished/unknown). */
internal data class MediaNextEpisode(
    val episode: Int,
    val airingAtEpochSeconds: Long,
    val timeUntilAiringSeconds: Long?,
)

internal data class MediaRank(
    val rank: Int,
    val type: String?,
    val context: String?,
    val format: String?,
    val season: String?,
    val year: Int?,
    val allTime: Boolean,
)

internal data class MediaTag(
    val name: String,
    val rank: Int,
    val isMediaSpoiler: Boolean,
)

internal data class MediaExternalLink(
    val site: String,
    val url: String,
    val type: String?,
    val iconUrl: String?,
    val color: String?,
    val language: String? = null,
)

/** The API marks the official site by name (its `type` is a generic INFO). */
internal fun MediaExternalLink.isOfficialSite(): Boolean =
    site.equals("Official Site", ignoreCase = true)

/** Localized label for a known external-link site name (API sends English). */
internal fun MediaExternalLink.siteLabel(strings: LanguageStrings): String =
    if (isOfficialSite()) strings.mediaDetailOfficialSiteLabel else site

internal data class MediaTrailer(
    val videoId: String,
    val site: String?,
    val thumbnailUrl: String?,
)

internal data class MediaRecommendationsPage(
    val items: List<MediaRecommendationEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

/** Localized work-release status (raw RELEASING → Airing/Publishing, etc.). */
internal fun MediaDetail.workStatusLabel(strings: LanguageStrings): String? {
    val parsed = parseMediaWorkStatus(status)
    return parsed?.label(strings, type == MediaDetailType.MANGA) ?: status
}

/** Canonical AniList page for this entry. */
internal fun MediaDetail.aniListUrl(): String {
    val segment = if (type == MediaDetailType.MANGA) "manga" else "anime"
    return "https://anilist.co/$segment/$id"
}

/** Body used by the share sheet: title + canonical URL. */
internal fun MediaDetail.shareText(): String = "$title\n${aniListUrl()}"

internal enum class MediaCharacterRole { MAIN, SUPPORTING, BACKGROUND, OTHER }

internal data class MediaCharacterEntry(
    val edgeKey: String,
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val role: MediaCharacterRole,
)

internal data class MediaCharactersPage(
    val items: List<MediaCharacterEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class MediaStaffEntry(
    val edgeKey: String,
    val staffId: Int,
    val name: String,
    val imageUrl: String?,
    val role: String?,
)

internal data class MediaStaffPage(
    val items: List<MediaStaffEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class MediaRelationKind {
    SEQUEL, PREQUEL, SIDE_STORY, SPIN_OFF, PARENT, ADAPTATION, ALTERNATIVE, SOURCE, SUMMARY, CHARACTER, OTHER
}

/** Localized relation label (strings live in the language tables, not here). */
internal fun MediaRelationKind.label(strings: LanguageStrings): String = when (this) {
    MediaRelationKind.SEQUEL -> strings.mediaRelationSequel
    MediaRelationKind.PREQUEL -> strings.mediaRelationPrequel
    MediaRelationKind.SIDE_STORY -> strings.mediaRelationSideStory
    MediaRelationKind.SPIN_OFF -> strings.mediaRelationSpinOff
    MediaRelationKind.PARENT -> strings.mediaRelationParent
    MediaRelationKind.ADAPTATION -> strings.mediaRelationAdaptation
    MediaRelationKind.ALTERNATIVE -> strings.mediaRelationAlternative
    MediaRelationKind.SOURCE -> strings.mediaRelationSource
    MediaRelationKind.SUMMARY -> strings.mediaRelationSummary
    MediaRelationKind.CHARACTER -> strings.mediaRelationCharacter
    MediaRelationKind.OTHER -> strings.mediaRelationOther
}

internal data class MediaDetailRelation(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val paletteHex: String?,
    val kind: MediaRelationKind,
    val format: String?,
    val season: MediaSeason? = null,
    val year: Int?,
    val workStatus: MediaWorkStatus? = null,
    val averageScore: Int?,
    val favourites: Int?,
    val viewerStatus: String?,
)

internal data class MediaRecommendationEntry(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val season: MediaSeason? = null,
    val year: Int?,
    val workStatus: MediaWorkStatus? = null,
    val episodesCount: Int?,
    val chaptersCount: Int?,
    val volumesCount: Int?,
    val averageScore: Int?,
    val favouritesCount: Int?,
    val likesCount: Int,
    val viewerStatus: String?,
)
