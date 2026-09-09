package com.luum.michi.app.notifications.domain.model

import com.luum.michi.app.core.language.domain.LanguageStrings

internal enum class NotificationFilter { ALL, AIRING, ACTIVITY, REPLIES, FORUM, FOLLOWS, MEDIA }

internal fun NotificationFilter.label(strings: LanguageStrings): String = when (this) {
    NotificationFilter.ALL -> strings.notificationFilterAll
    NotificationFilter.AIRING -> strings.notificationFilterAiring
    NotificationFilter.ACTIVITY -> strings.activityLabel
    NotificationFilter.REPLIES -> strings.notificationFilterReplies
    NotificationFilter.FORUM -> strings.forumLabel
    NotificationFilter.FOLLOWS -> strings.notificationFilterFollows
    NotificationFilter.MEDIA -> strings.notificationFilterMedia
}
