package com.luum.michi.app.core.model

import com.luum.michi.app.core.language.domain.LanguageStrings

internal enum class UserListSort {
    FOLLOW_LIST,
    TITLE,
    SCORE,
    PROGRESS,
    LAST_UPDATED,
    LAST_ADDED,
    START_DATE,
    COMPLETED_DATE,
    RELEASE_DATE,
    AVERAGE_SCORE,
    POPULARITY,
    FAVORITES,
    TRENDING,
    PRIORITY,
    NEXT_AIRING;

    fun label(strings: LanguageStrings): String = when (this) {
        FOLLOW_LIST -> strings.sortFollowList
        TITLE -> strings.sortTitle
        SCORE -> strings.sortScore
        PROGRESS -> strings.progressLabel
        LAST_UPDATED -> strings.sortLastUpdated
        LAST_ADDED -> strings.sortLastAdded
        START_DATE -> strings.sortStartDate
        COMPLETED_DATE -> strings.sortCompletedDate
        RELEASE_DATE -> strings.sortReleaseDate
        AVERAGE_SCORE -> strings.sortAverageScore
        POPULARITY -> strings.popularityLabel
        FAVORITES -> strings.favoritesLabel
        TRENDING -> strings.sortTrending
        PRIORITY -> strings.priorityLabel
        NEXT_AIRING -> strings.sortNextAiring
    }
}

internal enum class UserListOrder {
    ASCENDING,
    DESCENDING;

    fun label(strings: LanguageStrings): String = when (this) {
        ASCENDING -> strings.orderAscending
        DESCENDING -> strings.orderDescending
    }
}
