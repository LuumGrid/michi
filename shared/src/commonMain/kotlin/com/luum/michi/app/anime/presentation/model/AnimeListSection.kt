package com.luum.michi.app.anime.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class AnimeListSection {
    ALL,
    WATCHING,
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

internal val AnimeListSection.isCompleted: Boolean
    get() = when (this) {
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

internal fun AnimeListSection.modalLabel(strings: LanguageStrings): String = when (this) {
    AnimeListSection.COMPLETED_TV,
    AnimeListSection.COMPLETED_MOVIE,
    AnimeListSection.COMPLETED_OVA,
    AnimeListSection.COMPLETED_ONA,
    AnimeListSection.COMPLETED_TV_SHORT,
    AnimeListSection.COMPLETED_SPECIAL -> strings.sectionCompleted
    else -> label(strings)
}
