package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.label

internal enum class MangaListSection {
    ALL,
    CURRENT,
    COMPLETED,
    COMPLETED_MANGA,
    COMPLETED_NOVEL,
    COMPLETED_ONE_SHOT,
    PAUSED,
    DROPPED,
    PLANNING,
    REPEATING,
}

internal val MangaStatusSections: List<MangaListSection> = listOf(
    MangaListSection.CURRENT,
    MangaListSection.COMPLETED_MANGA,
    MangaListSection.COMPLETED_NOVEL,
    MangaListSection.COMPLETED_ONE_SHOT,
    MangaListSection.PAUSED,
    MangaListSection.DROPPED,
    MangaListSection.PLANNING,
    MangaListSection.REPEATING,
)

/**
 * Rail sections honoring the split toggle: merged COMPLETED when off,
 * per-format sections when on (AniList "split completed by format").
 */
internal fun mangaStatusSections(splitCompleted: Boolean): List<MangaListSection> =
    if (splitCompleted) {
        MangaStatusSections
    } else {
        MangaStatusSections.map { if (it.isCompleted) MangaListSection.COMPLETED else it }.distinct()
    }

internal val MangaListSection.isCompleted: Boolean
    get() = when (this) {
        MangaListSection.COMPLETED,
        MangaListSection.COMPLETED_MANGA,
        MangaListSection.COMPLETED_NOVEL,
        MangaListSection.COMPLETED_ONE_SHOT -> true
        MangaListSection.ALL,
        MangaListSection.CURRENT,
        MangaListSection.PAUSED,
        MangaListSection.DROPPED,
        MangaListSection.PLANNING,
        MangaListSection.REPEATING -> false
    }

internal fun MangaListSection.label(strings: LanguageStrings): String = when (this) {
    MangaListSection.ALL -> strings.sectionAll
    MangaListSection.CURRENT -> strings.sectionCurrent
    MangaListSection.COMPLETED -> strings.completedLabel
    MangaListSection.COMPLETED_MANGA ->
        "${strings.completedLabel} ${MediaFormat.MANGA.label(strings.formatUnknownLabel)}"
    MangaListSection.COMPLETED_NOVEL ->
        "${strings.completedLabel} ${MediaFormat.NOVEL.label(strings.formatUnknownLabel)}"
    MangaListSection.COMPLETED_ONE_SHOT ->
        "${strings.completedLabel} ${MediaFormat.ONE_SHOT.label(strings.formatUnknownLabel)}"
    MangaListSection.PAUSED -> strings.sectionPaused
    MangaListSection.DROPPED -> strings.sectionDropped
    MangaListSection.PLANNING -> strings.sectionPlanning
    MangaListSection.REPEATING -> strings.sectionRepeating
}

/** Raw list status → section (split decides the per-format COMPLETED). */
internal fun MediaListStatus.toMangaSection(format: MediaFormat, splitCompleted: Boolean): MangaListSection =
    when (this) {
        MediaListStatus.CURRENT -> MangaListSection.CURRENT
        MediaListStatus.COMPLETED -> format.completedMangaSection(splitCompleted)
        MediaListStatus.PAUSED -> MangaListSection.PAUSED
        MediaListStatus.DROPPED -> MangaListSection.DROPPED
        MediaListStatus.PLANNING -> MangaListSection.PLANNING
        MediaListStatus.REPEATING -> MangaListSection.REPEATING
    }

/** Section → status, the inverse of [toMangaSection] (for a progress save). */
internal fun MangaListSection.toMediaListStatus(): MediaListStatus = when (this) {
    MangaListSection.ALL,
    MangaListSection.CURRENT -> MediaListStatus.CURRENT
    MangaListSection.COMPLETED,
    MangaListSection.COMPLETED_MANGA,
    MangaListSection.COMPLETED_NOVEL,
    MangaListSection.COMPLETED_ONE_SHOT -> MediaListStatus.COMPLETED
    MangaListSection.PAUSED -> MediaListStatus.PAUSED
    MangaListSection.DROPPED -> MediaListStatus.DROPPED
    MangaListSection.PLANNING -> MediaListStatus.PLANNING
    MangaListSection.REPEATING -> MediaListStatus.REPEATING
}
