package com.luum.michi.app.mediaDetail.domain.media.model

import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.label
import com.luum.michi.app.core.language.domain.LanguageStrings
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
    val scoreDistribution: List<MediaScoreBucket>,
    val statusDistribution: List<MediaStatusBucket>,
    val characters: MediaCharactersPage,
    val staff: MediaStaffPage,
    val titleRomaji: String?,
    val titleNative: String?,
    val synonyms: List<String>,
    val rankings: List<MediaRank>,
    val tags: List<MediaTag>,
    val externalLinks: List<MediaExternalLink>,
    val trailer: MediaTrailer?,
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

internal data class MediaScoreBucket(val score: Int, val amount: Int)

internal enum class MediaStatsStatus {
    CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING, OTHER,
}

/** Localized stats-status label (reuses the list-status wording). */
internal fun MediaStatsStatus.label(strings: LanguageStrings, isManga: Boolean): String = when (this) {
    MediaStatsStatus.CURRENT -> MediaListStatus.CURRENT.label(strings, isManga)
    MediaStatsStatus.PLANNING -> MediaListStatus.PLANNING.label(strings, isManga)
    MediaStatsStatus.COMPLETED -> MediaListStatus.COMPLETED.label(strings, isManga)
    MediaStatsStatus.DROPPED -> MediaListStatus.DROPPED.label(strings, isManga)
    MediaStatsStatus.PAUSED -> MediaListStatus.PAUSED.label(strings, isManga)
    MediaStatsStatus.REPEATING -> MediaListStatus.REPEATING.label(strings, isManga)
    MediaStatsStatus.OTHER -> strings.mediaStatsStatusOther
}

internal data class MediaStatusBucket(val status: MediaStatsStatus, val amount: Int)

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

internal data class MediaVoiceActor(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val language: String?,
)

internal data class MediaCharacterEntry(
    val edgeKey: String,
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val role: MediaCharacterRole,
    val voiceActor: MediaVoiceActor?,
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
    val year: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val viewerStatus: String?,
)

internal data class MediaRecommendationEntry(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val year: Int?,
    val episodesCount: Int?,
    val chaptersCount: Int?,
    val volumesCount: Int?,
    val averageScore: Int?,
    val favouritesCount: Int?,
    val likesCount: Int,
    val viewerStatus: String?,
)
