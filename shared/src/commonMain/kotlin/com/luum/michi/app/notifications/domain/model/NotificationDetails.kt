package com.luum.michi.app.notifications.domain.model

import com.luum.michi.app.core.util.stripHtml

/**
 * The optional detail block a media-change / merge / deletion or submission
 * notification carries. Fields are already HTML-cleaned and never blank; the
 * status stays raw for the UI to translate.
 */
internal data class NotificationDetails(
    val reason: String?,
    val notes: String?,
    val status: String?,
    val affectedTitles: List<String>,
)

/** The kinds whose notifications can carry a meaningful detail block. */
private val detailKinds = setOf(
    NotificationKind.MEDIA_DATA_CHANGE,
    NotificationKind.MEDIA_MERGE,
    NotificationKind.MEDIA_DELETION,
    NotificationKind.MEDIA_SUBMISSION_UPDATE,
    NotificationKind.STAFF_SUBMISSION_UPDATE,
    NotificationKind.CHARACTER_SUBMISSION_UPDATE,
)

/**
 * Details to paint for this notification, or null when the kind has none (every
 * social kind, airing, follows, etc.) or all of its fields are blank.
 */
internal fun AppNotification.notificationDetails(): NotificationDetails? {
    if (kind !in detailKinds) return null
    val cleanReason = reason.cleanDetail()
    val cleanNotes = notes.cleanDetail()
    val cleanStatus = status.cleanDetail()
    val cleanTitles = deletedMediaTitles.mapNotNull { it.cleanDetail() }
    if (cleanReason == null && cleanNotes == null && cleanStatus == null && cleanTitles.isEmpty()) {
        return null
    }
    return NotificationDetails(
        reason = cleanReason,
        notes = cleanNotes,
        status = cleanStatus,
        affectedTitles = cleanTitles,
    )
}

private fun String?.cleanDetail(): String? =
    this?.stripHtml()?.takeIf { it.isNotBlank() }
