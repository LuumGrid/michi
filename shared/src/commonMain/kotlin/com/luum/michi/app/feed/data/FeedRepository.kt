package com.luum.michi.app.feed.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.feed.presentation.model.FeedActivity
import com.luum.michi.app.feed.presentation.model.FeedReviewsPage

internal enum class FeedFilter { FOLLOWING, GLOBAL }

internal enum class FeedSection { ACTIVITY, REVIEWS }

internal enum class FeedChip { FOLLOWING, GLOBAL, REVIEWS }

internal data class FeedPage(val activities: List<FeedActivity>, val hasNextPage: Boolean)

internal data class FeedActivityFilter(
    val statuses: Boolean = true,
    val animationProgress: Boolean = true,
    val readingProgress: Boolean = true,
    val messages: Boolean = false,
    val myActivities: Boolean = false,
)

internal interface FeedRepository {
    suspend fun loadFeed(filter: FeedFilter, activityFilter: FeedActivityFilter, page: Int, viewerId: Int): NetworkResult<FeedPage>
    suspend fun loadReviews(page: Int): NetworkResult<FeedReviewsPage>
}
