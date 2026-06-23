package com.luum.michi.app.notifications.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class NotificationFilter { ALL, AIRING, ACTIVITY, FORUM, FOLLOWS, MEDIA }

internal fun NotificationFilter.label(strings: LanguageStrings): String = when (this) {
    NotificationFilter.ALL -> strings.notificationFilterAll
    NotificationFilter.AIRING -> strings.notificationFilterAiring
    NotificationFilter.ACTIVITY -> strings.notificationFilterActivity
    NotificationFilter.FORUM -> strings.notificationFilterForum
    NotificationFilter.FOLLOWS -> strings.notificationFilterFollows
    NotificationFilter.MEDIA -> strings.notificationFilterMedia
}
