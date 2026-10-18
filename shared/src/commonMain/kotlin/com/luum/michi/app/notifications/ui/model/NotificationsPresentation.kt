package com.luum.michi.app.notifications.ui.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationCategory
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
 * The row's main line. Every branch falls back to a generic label so a missing
 * episode/title/user name never renders an empty string.
 */
private fun AppNotification.notificationText(strings: LanguageStrings): String = when (category) {
    NotificationCategory.AIRING -> airingText(strings)
    NotificationCategory.FOLLOWING -> userName.startedWith(strings.notifFollowingLabel)
    NotificationCategory.ACTIVITY -> userName.prefixedWith(strings.notifActivityLabel)
    NotificationCategory.REPLIES -> userName.prefixedWith(strings.notifForumLabel)
    NotificationCategory.FORUM -> userName.prefixedWith(strings.notifForumLabel)
    NotificationCategory.MESSAGE -> userName.prefixedWith(strings.notifMessageLabel)
    NotificationCategory.MEDIA_CHANGE -> mediaTitle.suffixedWith(strings.notifMediaChangeLabel)
    NotificationCategory.OTHER -> strings.otherLabel
}

/** "bob started following you". */
private fun String?.startedWith(label: String): String =
    this?.takeIf { it.isNotBlank() }?.let { "$it $label" } ?: label

/** "bob · New activity". */
private fun String?.prefixedWith(label: String): String =
    this?.takeIf { it.isNotBlank() }?.let { "$it · $label" } ?: label

/** "A title on your list changed: Naruto". */
private fun String?.suffixedWith(label: String): String =
    this?.takeIf { it.isNotBlank() }?.let { "$label: $it" } ?: label

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

/** Relative up to 7 days, then the shared readable date (never a dead label). */
internal fun notificationTimeLabel(
    nowEpochSeconds: Long,
    createdEpochSeconds: Long,
    strings: LanguageStrings,
): String {
    val age = (nowEpochSeconds - createdEpochSeconds).coerceAtLeast(0)
    return when {
        age < 60 -> strings.timeJustNowLabel
        age < 3_600 -> strings.timeMinutesAgoLabel((age / 60).toInt())
        age < 86_400 -> strings.timeHoursAgoLabel((age / 3_600).toInt())
        age < 7 * 86_400 -> strings.timeDaysAgoLabel((age / 86_400).toInt())
        else -> strings.notificationDateLabel(createdEpochSeconds.toLocalMediaReleaseDateTime())
    }
}

/** Sends a tapped notification to the media detail or the browser. */
internal fun NotificationTarget.dispatch(
    onOpenMedia: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    when (this) {
        is NotificationTarget.Media -> onOpenMedia(id)
        is NotificationTarget.Web -> onOpenUrl(url)
    }
}
