package com.luum.michi.app.notifications.ui.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationKind
import com.luum.michi.app.notifications.domain.model.NotificationTarget

/** One notification shaped for the row: image, text, relative time, destination. */
internal data class NotificationRowItem(
    val id: Int,
    val image: CoverSizes?,
    val text: String,
    val time: String,
    val target: NotificationTarget,
)

internal fun AppNotification.toRowItem(
    strings: LanguageStrings,
    nowEpochSeconds: Long,
): NotificationRowItem = NotificationRowItem(
    id = id,
    image = image,
    text = notificationText(strings),
    time = notificationTimeLabel(nowEpochSeconds, createdAtEpochSeconds, strings),
    target = target,
)

/**
 * The row's main line. The exact [NotificationKind] picks the phrase; each
 * LanguageStrings function owns the wording and its own fallbacks, so no sentence
 * is assembled here (airing keeps its own two-part label as before).
 */
private fun AppNotification.notificationText(strings: LanguageStrings): String = when (kind) {
    NotificationKind.AIRING -> airingText(strings)
    NotificationKind.FOLLOWING -> strings.notifFollowingText(userName)
    NotificationKind.ACTIVITY_LIKE -> strings.notifActivityLikeText(userName)
    NotificationKind.ACTIVITY_REPLY -> strings.notifActivityReplyText(userName)
    NotificationKind.ACTIVITY_REPLY_LIKE -> strings.notifActivityReplyLikeText(userName)
    NotificationKind.ACTIVITY_REPLY_SUBSCRIBED -> strings.notifActivityReplySubscribedText(userName)
    NotificationKind.ACTIVITY_MENTION -> strings.notifActivityMentionText(userName)
    NotificationKind.ACTIVITY_MESSAGE -> strings.notifActivityMessageText(userName)
    NotificationKind.THREAD_COMMENT_REPLY -> strings.notifThreadCommentReplyText(userName, threadTitle)
    NotificationKind.THREAD_COMMENT_MENTION -> strings.notifThreadCommentMentionText(userName, threadTitle)
    NotificationKind.THREAD_SUBSCRIBED -> strings.notifThreadSubscribedText(userName, threadTitle)
    NotificationKind.THREAD_COMMENT_LIKE -> strings.notifThreadCommentLikeText(userName, threadTitle)
    NotificationKind.THREAD_LIKE -> strings.notifThreadLikeText(userName, threadTitle)
    NotificationKind.RELATED_MEDIA_ADDITION -> strings.notifRelatedMediaAdditionText(mediaTitle)
    NotificationKind.MEDIA_DATA_CHANGE -> strings.notifMediaDataChangeText(mediaTitle)
    NotificationKind.MEDIA_MERGE -> strings.notifMediaMergeText(mediaTitle, deletedMediaTitles)
    NotificationKind.MEDIA_DELETION -> strings.notifMediaDeletionText(mediaTitle)
    NotificationKind.MEDIA_SUBMISSION_UPDATE,
    NotificationKind.STAFF_SUBMISSION_UPDATE,
    NotificationKind.CHARACTER_SUBMISSION_UPDATE -> strings.notifSubmissionText(subjectName, status)
    NotificationKind.UNKNOWN -> strings.otherLabel
}

private fun AppNotification.airingText(strings: LanguageStrings): String {
    val title = mediaTitle?.takeIf { it.isNotBlank() }
    val episodeNumber = episode?.takeIf { it > 0 }
    return when {
        episodeNumber != null && title != null ->
            "${strings.notifAiringPrefix} $episodeNumber ${strings.notifAiringAired} $title"
        title != null -> title
        episodeNumber != null -> "${strings.notifAiringPrefix} $episodeNumber"
        else -> strings.notificationsAiringTitle
    }
}

/**
 * Time since [createdEpochSeconds], in the past: the readable date plus, in
 * parentheses, the elapsed time (under a minute is "now", up to a week mirrors
 * the airing countdown label's two-unit split). From 7 days on, just the date.
 * The sentence is assembled by LanguageStrings, exactly like the next-episode
 * release label.
 */
internal fun notificationTimeLabel(
    nowEpochSeconds: Long,
    createdEpochSeconds: Long,
    strings: LanguageStrings,
): String {
    val age = (nowEpochSeconds - createdEpochSeconds).coerceAtLeast(0)
    val relative = when {
        age < 60 -> strings.timeJustNowLabel
        age >= 7 * 86_400 -> null
        else -> strings.timeAgoLabel(
            days = (age / 86_400).toInt(),
            hours = ((age % 86_400) / 3_600).toInt(),
            minutes = ((age % 3_600) / 60).toInt(),
        )
    }
    return strings.notificationTimeLabel(
        strings.notificationDateLabel(createdEpochSeconds.toLocalMediaReleaseDateTime()),
        relative,
    )
}

/** Sends a tapped notification to the right destination ([None] does nothing). */
internal fun NotificationTarget.dispatch(
    onOpenMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    when (this) {
        is NotificationTarget.Media -> onOpenMedia(id)
        is NotificationTarget.Character -> onOpenCharacter(id)
        is NotificationTarget.Staff -> onOpenStaff(id)
        is NotificationTarget.Web -> onOpenUrl(url)
        NotificationTarget.None -> Unit
    }
}
