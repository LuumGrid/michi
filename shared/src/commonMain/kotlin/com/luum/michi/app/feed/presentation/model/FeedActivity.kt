package com.luum.michi.app.feed.presentation.model

import androidx.compose.runtime.Immutable

internal sealed interface FeedActivity {
    val id: Int
    val userId: Int
    val userName: String
    val userAvatarUrl: String?
    val createdAtEpochSeconds: Long
    val likeCount: Int
    val replyCount: Int
    val siteUrl: String?

    @Immutable
    data class MediaList(
        override val id: Int,
        override val userId: Int,
        override val userName: String,
        override val userAvatarUrl: String?,
        override val createdAtEpochSeconds: Long,
        override val likeCount: Int,
        override val replyCount: Int,
        override val siteUrl: String?,
        val mediaId: Int,
        val mediaTitle: String,
        val coverUrl: String?,
        val isAnime: Boolean,
        val statusText: String,
    ) : FeedActivity

    @Immutable
    data class Text(
        override val id: Int,
        override val userId: Int,
        override val userName: String,
        override val userAvatarUrl: String?,
        override val createdAtEpochSeconds: Long,
        override val likeCount: Int,
        override val replyCount: Int,
        override val siteUrl: String?,
        val text: String,
    ) : FeedActivity

    @Immutable
    data class Message(
        override val id: Int,
        override val userId: Int,
        override val userName: String,
        override val userAvatarUrl: String?,
        override val createdAtEpochSeconds: Long,
        override val likeCount: Int,
        override val replyCount: Int,
        override val siteUrl: String?,
        val messengerName: String,
        val recipientName: String,
        val message: String,
    ) : FeedActivity
}
