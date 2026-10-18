package com.luum.michi.app.notifications.domain.model

/**
 * One entry per AniList `NotificationType`: the exact API type, so the text and
 * the filter keep it instead of collapsing it into a coarse bucket.
 *
 * [apiType] is the value AniList returns in `type` and accepts in `type_in`; it
 * is null only for [UNKNOWN], a type we do not model yet — it is shown with the
 * generic label and never sent as a filter.
 */
internal enum class NotificationKind(val apiType: String?) {
    AIRING("AIRING"),
    FOLLOWING("FOLLOWING"),
    ACTIVITY_LIKE("ACTIVITY_LIKE"),
    ACTIVITY_REPLY("ACTIVITY_REPLY"),
    ACTIVITY_REPLY_LIKE("ACTIVITY_REPLY_LIKE"),
    ACTIVITY_REPLY_SUBSCRIBED("ACTIVITY_REPLY_SUBSCRIBED"),
    ACTIVITY_MENTION("ACTIVITY_MENTION"),
    ACTIVITY_MESSAGE("ACTIVITY_MESSAGE"),
    THREAD_COMMENT_REPLY("THREAD_COMMENT_REPLY"),
    THREAD_COMMENT_MENTION("THREAD_COMMENT_MENTION"),
    THREAD_SUBSCRIBED("THREAD_SUBSCRIBED"),
    THREAD_COMMENT_LIKE("THREAD_COMMENT_LIKE"),
    THREAD_LIKE("THREAD_LIKE"),
    RELATED_MEDIA_ADDITION("RELATED_MEDIA_ADDITION"),
    MEDIA_DATA_CHANGE("MEDIA_DATA_CHANGE"),
    MEDIA_MERGE("MEDIA_MERGE"),
    MEDIA_DELETION("MEDIA_DELETION"),
    MEDIA_SUBMISSION_UPDATE("MEDIA_SUBMISSION_UPDATE"),
    STAFF_SUBMISSION_UPDATE("STAFF_SUBMISSION_UPDATE"),
    CHARACTER_SUBMISSION_UPDATE("CHARACTER_SUBMISSION_UPDATE"),
    UNKNOWN(null),
    ;

    companion object {
        /** The kind for an API `type` string, or [UNKNOWN] — never a dropped row. */
        fun ofApiType(type: String): NotificationKind =
            entries.firstOrNull { it.apiType == type } ?: UNKNOWN
    }
}
