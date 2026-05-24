package com.luum.michi.app.mediaDetail.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class MediaListStatus { CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING }

internal fun MediaListStatus.toApiValue(): String = name

internal fun parseMediaListStatus(raw: String?): MediaListStatus? = when (raw) {
    "CURRENT" -> MediaListStatus.CURRENT
    "PLANNING" -> MediaListStatus.PLANNING
    "COMPLETED" -> MediaListStatus.COMPLETED
    "DROPPED" -> MediaListStatus.DROPPED
    "PAUSED" -> MediaListStatus.PAUSED
    "REPEATING" -> MediaListStatus.REPEATING
    else -> null
}

internal fun MediaListStatus.label(strings: LanguageStrings, isManga: Boolean): String = when (this) {
    MediaListStatus.CURRENT -> if (isManga) strings.sectionReading else strings.sectionWatching
    MediaListStatus.PLANNING -> strings.sectionPlanning
    MediaListStatus.COMPLETED -> strings.sectionCompleted
    MediaListStatus.DROPPED -> strings.sectionDropped
    MediaListStatus.PAUSED -> strings.sectionPaused
    MediaListStatus.REPEATING -> if (isManga) strings.sectionRereading else strings.sectionRewatching
}
