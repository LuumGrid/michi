package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class MangaListSection {
    ALL,
    CURRENT,
    COMPLETED,
    PAUSED,
    DROPPED,
    PLANNING,
    REPEATING,
}

internal val MangaStatusSections: List<MangaListSection> = listOf(
    MangaListSection.CURRENT,
    MangaListSection.COMPLETED,
    MangaListSection.PAUSED,
    MangaListSection.DROPPED,
    MangaListSection.PLANNING,
    MangaListSection.REPEATING,
)

internal fun MangaListSection.label(strings: LanguageStrings): String = when (this) {
    MangaListSection.ALL -> strings.sectionAll
    MangaListSection.CURRENT -> strings.sectionCurrent
    MangaListSection.COMPLETED -> strings.sectionCompleted
    MangaListSection.PAUSED -> strings.sectionPaused
    MangaListSection.DROPPED -> strings.sectionDropped
    MangaListSection.PLANNING -> strings.sectionPlanning
    MangaListSection.REPEATING -> strings.sectionRepeating
}
