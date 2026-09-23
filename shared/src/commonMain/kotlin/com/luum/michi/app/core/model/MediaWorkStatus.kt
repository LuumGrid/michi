package com.luum.michi.app.core.model

import com.luum.michi.app.core.language.domain.LanguageStrings

/** Release status of the work itself (not the user's list status). AniList
 *  uses RELEASING for both airing anime and publishing manga; the label
 *  splits by type ([isManga]). Unknown raws parse to null — callers omit
 *  the status instead of showing a placeholder. */
internal enum class MediaWorkStatus {
    FINISHED,
    RELEASING,
    NOT_YET_RELEASED,
    CANCELLED,
    HIATUS,
}

internal fun parseMediaWorkStatus(raw: String?): MediaWorkStatus? = when (raw?.uppercase()) {
    "FINISHED" -> MediaWorkStatus.FINISHED
    "RELEASING" -> MediaWorkStatus.RELEASING
    "NOT_YET_RELEASED" -> MediaWorkStatus.NOT_YET_RELEASED
    "CANCELLED" -> MediaWorkStatus.CANCELLED
    "HIATUS" -> MediaWorkStatus.HIATUS
    else -> null
}

internal fun MediaWorkStatus.label(strings: LanguageStrings, isManga: Boolean): String = when (this) {
    MediaWorkStatus.FINISHED -> strings.workStatusFinished
    MediaWorkStatus.RELEASING -> if (isManga) strings.workStatusPublishing else strings.workStatusAiring
    MediaWorkStatus.NOT_YET_RELEASED -> strings.workStatusUpcoming
    MediaWorkStatus.CANCELLED -> strings.workStatusCancelled
    MediaWorkStatus.HIATUS -> strings.workStatusHiatus
}
