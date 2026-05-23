package com.luum.michi.app.animation.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class AnimationListSection {
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

internal val AnimationStatusSections: List<AnimationListSection> = listOf(
    AnimationListSection.WATCHING,
    AnimationListSection.COMPLETED_TV,
    AnimationListSection.COMPLETED_MOVIE,
    AnimationListSection.COMPLETED_OVA,
    AnimationListSection.COMPLETED_ONA,
    AnimationListSection.COMPLETED_TV_SHORT,
    AnimationListSection.COMPLETED_SPECIAL,
    AnimationListSection.PAUSED,
    AnimationListSection.DROPPED,
    AnimationListSection.PLANNING,
    AnimationListSection.REWATCHING,
)

internal val AnimationListSection.isCompleted: Boolean
    get() = when (this) {
        AnimationListSection.COMPLETED_TV,
        AnimationListSection.COMPLETED_MOVIE,
        AnimationListSection.COMPLETED_OVA,
        AnimationListSection.COMPLETED_ONA,
        AnimationListSection.COMPLETED_TV_SHORT,
        AnimationListSection.COMPLETED_SPECIAL -> true
        AnimationListSection.ALL,
        AnimationListSection.WATCHING,
        AnimationListSection.PAUSED,
        AnimationListSection.DROPPED,
        AnimationListSection.PLANNING,
        AnimationListSection.REWATCHING -> false
    }

internal fun AnimationListSection.label(strings: LanguageStrings): String = when (this) {
    AnimationListSection.ALL -> strings.sectionAll
    AnimationListSection.WATCHING -> strings.sectionWatching
    AnimationListSection.COMPLETED_TV -> strings.sectionCompletedTv
    AnimationListSection.COMPLETED_MOVIE -> strings.sectionCompletedMovie
    AnimationListSection.COMPLETED_OVA -> strings.sectionCompletedOva
    AnimationListSection.COMPLETED_ONA -> strings.sectionCompletedOna
    AnimationListSection.COMPLETED_TV_SHORT -> strings.sectionCompletedTvShort
    AnimationListSection.COMPLETED_SPECIAL -> strings.sectionCompletedSpecial
    AnimationListSection.PAUSED -> strings.sectionPaused
    AnimationListSection.DROPPED -> strings.sectionDropped
    AnimationListSection.PLANNING -> strings.sectionPlanning
    AnimationListSection.REWATCHING -> strings.sectionRewatching
}

internal fun AnimationListSection.modalLabel(strings: LanguageStrings): String = when (this) {
    AnimationListSection.COMPLETED_TV,
    AnimationListSection.COMPLETED_MOVIE,
    AnimationListSection.COMPLETED_OVA,
    AnimationListSection.COMPLETED_ONA,
    AnimationListSection.COMPLETED_TV_SHORT,
    AnimationListSection.COMPLETED_SPECIAL -> strings.sectionCompleted
    else -> label(strings)
}
