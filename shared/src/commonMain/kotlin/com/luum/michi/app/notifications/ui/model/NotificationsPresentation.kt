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
    /** The image is the actor's avatar (not a media cover): draw it as a person. */
    val isPerson: Boolean,
    val initials: String,
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
    isPerson = isPersonCategory(),
    initials = userName?.trim()?.take(1)?.uppercase().orEmpty(),
    text = notificationText(strings),
    time = notificationTimeLabel(nowEpochSeconds, createdAtEpochSeconds, strings),
    target = target,
)

/** A media cover is only carried by the airing and media-change notifications. */
private fun AppNotification.isPersonCategory(): Boolean =
    category != NotificationCategory.AIRING && category != NotificationCategory.MEDIA_CHANGE

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
        age < 3_600 -> strings.timeAgoLabel("${age / 60}min")
        age < 86_400 -> strings.timeAgoLabel("${age / 3_600}h")
        age < 7 * 86_400 -> strings.timeAgoLabel("${age / 86_400}d")
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
