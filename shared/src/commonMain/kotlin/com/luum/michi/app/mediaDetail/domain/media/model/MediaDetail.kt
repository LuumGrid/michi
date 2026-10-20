package com.luum.michi.app.mediaDetail.domain.media.model

import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.medialist.domain.label
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.core.model.Page

internal enum class MediaDetailType { ANIME, MANGA, UNKNOWN }

/**
 * The catalog this entry belongs to, or null when AniList did not say
 * ([MediaDetailType.UNKNOWN]). A genre page needs a real catalog, so an unknown
 * type leaves its genre chips inert instead of guessing.
 */
internal fun MediaDetailType.toMediaType(): MediaType? = when (this) {
    MediaDetailType.ANIME -> MediaType.ANIME
    MediaDetailType.MANGA -> MediaType.MANGA
    MediaDetailType.UNKNOWN -> null
}

internal data class StudioRef(val id: Int, val name: String)

internal data class MediaDetail(
    val id: Int,
    val type: MediaDetailType,
    val title: String,
    val cover: CoverSizes?,
    val bannerUrl: String?,
    val paletteHex: String?,
    val format: String?,
    val status: MediaWorkStatus?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val duration: Int?,
    val genres: List<String>,
    val studios: List<StudioRef>,
    val producers: List<StudioRef>,
    val source: String?,
    val season: String?,
    val startedLabel: String?,
    val endedLabel: String?,
    val averageScore: Int?,
    val meanScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val descriptionPlain: String,
    /** Hidden spoiler ranges over [descriptionPlain] (inclusive, empty when none). */
    val descriptionSpoilers: List<IntRange> = emptyList(),
    val isAdult: Boolean,
    val isFavourite: Boolean,
    val viewerEntry: MediaListViewerEntry?,
    val relations: List<MediaDetailRelation>,
    val characters: Page<MediaCharacterEntry>,
    val staff: Page<MediaStaffEntry>,
    val recommendations: Page<MediaRecommendationEntry>,
    val titleRomaji: String?,
    val titleNative: String?,
    val synonyms: List<String>,
    val tags: List<MediaTag>,
    val externalLinks: List<ExternalLink>,
    val trailer: MediaTrailer?,
    val nextEpisode: MediaNextEpisode? = null,
)

/** Next airing episode of a releasing show (null for finished/unknown). */
internal data class MediaNextEpisode(
    val episode: Int,
    val airingAtEpochSeconds: Long,
    val timeUntilAiringSeconds: Long?,
)

/**
 * The next episode only while its drop is still ahead of the user: a stale
 * `timeUntilAiring` (already aired) is not worth showing.
 */
internal fun MediaDetail.upcomingNextEpisode(): MediaNextEpisode? =
    nextEpisode?.takeIf { (it.timeUntilAiringSeconds ?: 0L) > 0L }

internal data class MediaTag(
    val name: String,
    val rank: Int,
    val isMediaSpoiler: Boolean,
)

/** The Tags section exists whenever the media has any tag, spoilers included. */
internal fun List<MediaTag>.hasTags(): Boolean = isNotEmpty()

/**
 * Tags to paint: without [showSpoilers] the spoiler ones are filtered out, and
 * spoiler tags always come after the regular ones (the sort is stable, so each
 * group keeps the server order).
 */
internal fun List<MediaTag>.orderedTags(showSpoilers: Boolean): List<MediaTag> {
    val visible = if (showSpoilers) this else filterNot { it.isMediaSpoiler }
    return visible.sortedBy { it.isMediaSpoiler }
}

internal data class MediaTrailer(
    val videoId: String,
    val site: String?,
    val thumbnailUrl: String?,
)

/** Localized work-release status (RELEASING → Airing/Publishing, etc.). */
internal fun MediaDetail.workStatusLabel(strings: LanguageStrings): String? =
    status?.label(strings, type == MediaDetailType.MANGA)

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
    val image: CoverSizes?,
    val role: MediaCharacterRole,
)

internal data class MediaStaffEntry(
    val edgeKey: String,
    val staffId: Int,
    val name: String,
    val image: CoverSizes?,
    val role: String?,
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
    val cover: CoverSizes?,
    val kind: MediaRelationKind,
    val format: String?,
    val season: MediaSeason? = null,
    val year: Int?,
    val workStatus: MediaWorkStatus? = null,
)

internal data class MediaRecommendationEntry(
    val id: Int,
    val title: String,
    val cover: CoverSizes?,
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
