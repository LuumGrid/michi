package com.luum.michi.app.notifications.repository

import com.luum.michi.app.core.media.repository.bestTitleOrNull
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.NotificationKind
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.domain.model.kinds
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.person.repository.toCoverSizes
import com.luum.michi.app.core.model.CoverSizes

internal fun NotificationNodeDto.toAppNotification(): AppNotification? {
    val notificationId = id ?: return null
    val notificationType = type ?: return null
    val kind = NotificationKind.ofApiType(notificationType)

    val image = media?.coverImage?.toCoverSizes()
        ?: staff?.image?.toCoverSizes()
        ?: character?.image?.toCoverSizes()
        ?: user?.avatar?.let { CoverSizes(medium = it.medium, large = it.large) }

    val target: NotificationTarget = when {
        // A deleted title has nowhere to go; neither has a rejected submission.
        kind == NotificationKind.MEDIA_DELETION -> NotificationTarget.None
        media?.id != null -> NotificationTarget.Media(media.id)
        staff?.id != null -> NotificationTarget.Staff(staff.id)
        character?.id != null -> NotificationTarget.Character(character.id)
        kind == NotificationKind.MEDIA_SUBMISSION_UPDATE -> NotificationTarget.None
        else -> webTarget(kind)
    }

    return AppNotification(
        id = notificationId,
        kind = kind,
        image = image,
        createdAtEpochSeconds = createdAt ?: 0L,
        target = target,
        episode = episode,
        // Every text field is normalized: a blank never reaches LanguageStrings,
        // which relies on null to pick its fallback wording.
        mediaTitle = (media?.title.bestTitleOrNull() ?: deletedMediaTitle)?.takeIf { it.isNotBlank() },
        threadTitle = thread?.title?.takeIf { it.isNotBlank() },
        userName = user?.name?.takeIf { it.isNotBlank() },
        subjectName = submissionSubject(kind),
        reason = reason,
        notes = notes,
        status = status,
        deletedMediaTitles = deletedMediaTitles.filter { it.isNotBlank() },
    )
}

/** The submitted item's name: the submitted title (falling back to the media
 *  title), or the staff/character name. Only the submission kinds carry one. */
private fun NotificationNodeDto.submissionSubject(kind: NotificationKind): String? = when (kind) {
    NotificationKind.MEDIA_SUBMISSION_UPDATE ->
        submittedTitle?.takeIf { it.isNotBlank() } ?: media?.title.bestTitleOrNull()
    NotificationKind.STAFF_SUBMISSION_UPDATE -> staff?.name?.bestName?.takeIf { it.isNotBlank() }
    NotificationKind.CHARACTER_SUBMISSION_UPDATE -> character?.name?.bestName?.takeIf { it.isNotBlank() }
    else -> null
}

/** Fallback destination: any notification without a usable id still opens the feed. */
private const val AniListNotificationsUrl = "https://anilist.co/notifications"

/**
 * The web destination of a non-media notification, decided by the data it
 * carries (all URLs are built here, the single place):
 *  - an activity, reply, reply-like, mention, reply-subscribed or message has
 *    `activityId` → that activity;
 *  - a forum reply/like/subscribed has `thread { id }` (+ `commentId`) or a
 *    thread like has `threadId` → the thread, with `/comment/{id}` when known;
 *  - a follow has no id, only the actor → the user profile;
 *  - anything else (an unmodelled type) → the notifications feed, the only place
 *    the user can see what it is.
 */
private fun NotificationNodeDto.webTarget(kind: NotificationKind): NotificationTarget {
    activityId?.let { return NotificationTarget.Web("https://anilist.co/activity/$it") }
    val forumThreadId = thread?.id ?: threadId
    if (forumThreadId != null) {
        val forumCommentId = commentId
        val commentPath = if (forumCommentId != null && forumCommentId > 0) "/comment/$forumCommentId" else ""
        return NotificationTarget.Web("https://anilist.co/forum/thread/$forumThreadId$commentPath")
    }
    val name = user?.name
    if (kind == NotificationKind.FOLLOWING && !name.isNullOrBlank()) {
        return NotificationTarget.Web("https://anilist.co/user/$name")
    }
    return NotificationTarget.Web(AniListNotificationsUrl)
}

/** The API `type_in` list of a filter, or null for ALL (the unfiltered feed). */
internal fun NotificationFilter.toAniListTypes(): List<String>? =
    kinds()?.mapNotNull { it.apiType }
