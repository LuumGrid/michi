package com.luum.michi.app.core.model

/**
 * Derives the AniList `MediaSort` key from the shared sort sheet selection
 * ([UserListSort] + [UserListOrder]). Criteria with no exact API equivalent fall
 * back to the closest one. [isAnime] picks the progress key (`EPISODES` vs
 * `CHAPTERS`); every other criterion is media-type agnostic. Pure, so both the
 * Explore search and the studio media grid derive their server sort here.
 */
internal fun UserListSort.toMediaSort(order: UserListOrder, isAnime: Boolean): String {
    val descending = order == UserListOrder.DESCENDING
    fun both(asc: String, desc: String) = if (descending) desc else asc
    return when (this) {
        UserListSort.FOLLOW_LIST -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.TITLE -> both("TITLE_ROMAJI", "TITLE_ROMAJI_DESC")
        UserListSort.SCORE -> both("SCORE", "SCORE_DESC")
        UserListSort.PROGRESS -> if (isAnime) {
            both("EPISODES", "EPISODES_DESC")
        } else {
            both("CHAPTERS", "CHAPTERS_DESC")
        }
        UserListSort.LAST_UPDATED -> both("UPDATED_AT", "UPDATED_AT_DESC")
        UserListSort.LAST_ADDED -> both("ID", "ID_DESC")
        UserListSort.START_DATE -> both("START_DATE", "START_DATE_DESC")
        UserListSort.COMPLETED_DATE -> both("END_DATE", "END_DATE_DESC")
        UserListSort.RELEASE_DATE -> both("START_DATE", "START_DATE_DESC")
        UserListSort.AVERAGE_SCORE -> both("SCORE", "SCORE_DESC")
        UserListSort.POPULARITY -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.FAVORITES -> both("FAVOURITES", "FAVOURITES_DESC")
        UserListSort.TRENDING -> both("TRENDING", "TRENDING_DESC")
        UserListSort.PRIORITY -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.NEXT_AIRING -> both("START_DATE", "START_DATE_DESC")
    }
}
