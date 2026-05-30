package com.luum.michi.app.feed.presentation.model

internal data class FeedReview(
    val id: Int,
    val summary: String,
    val rating: String,
    val reviewerName: String,
    val reviewerImageUrl: String?,
    val mediaId: Int,
    val mediaTitle: String,
)

internal data class FeedReviewsPage(
    val reviews: List<FeedReview>,
    val hasNextPage: Boolean,
)
