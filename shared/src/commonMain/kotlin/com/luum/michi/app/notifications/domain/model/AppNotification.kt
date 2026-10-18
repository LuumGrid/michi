package com.luum.michi.app.notifications.domain.model
import com.luum.michi.app.core.model.CoverSizes


/** Where tapping a notification goes. [None] is a real, inert destination. */
internal sealed interface NotificationTarget {
    data class Media(val id: Int) : NotificationTarget
    data class Character(val id: Int) : NotificationTarget
    data class Staff(val id: Int) : NotificationTarget
    data class Web(val url: String) : NotificationTarget

    /** Nothing to open: a deleted title, a rejected media submission. */
    data object None : NotificationTarget
}

internal data class AppNotification(
    val id: Int,
    /** The exact AniList type (never collapsed): drives both text and filters. */
    val kind: NotificationKind,
    val image: CoverSizes?,
    val createdAtEpochSeconds: Long,
    val target: NotificationTarget,
    val episode: Int? = null,
    /** Airing / related / media-change / merge / deletion: the media's title. */
    val mediaTitle: String? = null,
    /** Forum kinds: the thread's title. */
    val threadTitle: String? = null,
    /** The actor (follow, likes, replies, mentions, messages). */
    val userName: String? = null,
    /** Submissions: the submitted title / staff / character name. */
    val subjectName: String? = null,
    // Carried for a later UI; not painted yet.
    val reason: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val deletedMediaTitles: List<String> = emptyList(),
)
