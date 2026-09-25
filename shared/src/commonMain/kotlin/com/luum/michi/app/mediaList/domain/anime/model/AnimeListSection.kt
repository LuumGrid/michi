package com.luum.michi.app.mediaList.domain.anime.model

import com.luum.michi.app.core.language.domain.LanguageStrings

internal enum class AnimeListSection {
    ALL,
    WATCHING,
    COMPLETED,
    COMPLETED_TV,
    COMPLETED_MOVIE,
    COMPLETED_OVA,
    COMPLETED_ONA,
    COMPLETED_TV_SHORT,
    COMPLETED_SPECIAL,
    PAUSED,
    DROPPED,
    PLANNING,
    REWATCHING,
}

internal val AnimeStatusSections: List<AnimeListSection> = listOf(
    AnimeListSection.WATCHING,
    AnimeListSection.COMPLETED_TV,
    AnimeListSection.COMPLETED_MOVIE,
    AnimeListSection.COMPLETED_OVA,
    AnimeListSection.COMPLETED_ONA,
    AnimeListSection.COMPLETED_TV_SHORT,
    AnimeListSection.COMPLETED_SPECIAL,
    AnimeListSection.PAUSED,
    AnimeListSection.DROPPED,
    AnimeListSection.PLANNING,
    AnimeListSection.REWATCHING,
)

/**
 * Rail sections honoring the split toggle: merged COMPLETED when off,
 * per-format sections when on (AniList "split completed by format").
 */
internal fun animeStatusSections(splitCompleted: Boolean): List<AnimeListSection> =
    if (splitCompleted) {
        AnimeStatusSections
    } else {
        AnimeStatusSections.map { if (it.isCompleted) AnimeListSection.COMPLETED else it }.distinct()
    }

internal val AnimeListSection.isCompleted: Boolean
    get() = when (this) {
        AnimeListSection.COMPLETED,
        AnimeListSection.COMPLETED_TV,
        AnimeListSection.COMPLETED_MOVIE,
        AnimeListSection.COMPLETED_OVA,
        AnimeListSection.COMPLETED_ONA,
        AnimeListSection.COMPLETED_TV_SHORT,
        AnimeListSection.COMPLETED_SPECIAL -> true
        AnimeListSection.ALL,
        AnimeListSection.WATCHING,
        AnimeListSection.PAUSED,
        AnimeListSection.DROPPED,
        AnimeListSection.PLANNING,
        AnimeListSection.REWATCHING -> false
    }

internal fun AnimeListSection.label(strings: LanguageStrings): String = when (this) {
    AnimeListSection.ALL -> strings.sectionAll
    AnimeListSection.WATCHING -> strings.sectionWatching
    AnimeListSection.COMPLETED -> strings.completedLabel
    AnimeListSection.COMPLETED_TV -> strings.sectionCompletedTv
    AnimeListSection.COMPLETED_MOVIE -> strings.sectionCompletedMovie
    AnimeListSection.COMPLETED_OVA -> strings.sectionCompletedOva
    AnimeListSection.COMPLETED_ONA -> strings.sectionCompletedOna
    AnimeListSection.COMPLETED_TV_SHORT -> strings.sectionCompletedTvShort
    AnimeListSection.COMPLETED_SPECIAL -> strings.sectionCompletedSpecial
    AnimeListSection.PAUSED -> strings.sectionPaused
    AnimeListSection.DROPPED -> strings.sectionDropped
    AnimeListSection.PLANNING -> strings.sectionPlanning
    AnimeListSection.REWATCHING -> strings.sectionRewatching
}
