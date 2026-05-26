package com.luum.michi.app.feed.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.core.util.relativeTime
import com.luum.michi.app.feed.presentation.model.FeedActivity

@Composable
internal fun FeedActivityCard(
    activity: FeedActivity,
    nowEpochSeconds: Long,
    onMediaClick: (mediaId: Int, isAnime: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = activity.userAvatarUrl,
                    contentDescription = activity.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = activity.userName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = relativeTime(activity.createdAtEpochSeconds, nowEpochSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (activity) {
                is FeedActivity.MediaList -> MediaListContent(
                    activity = activity,
                    onMediaClick = onMediaClick,
                )
                is FeedActivity.Text -> TextContent(activity = activity)
                is FeedActivity.Message -> MessageContent(activity = activity)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaCount(
                    icon = { Icon(painter = PlatformIcons.Like, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    count = activity.likeCount,
                )
                MetaCount(
                    icon = { Icon(painter = PlatformIcons.Comments, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    count = activity.replyCount,
                )
            }
        }
    }
}

@Composable
private fun MediaListContent(
    activity: FeedActivity.MediaList,
    onMediaClick: (mediaId: Int, isAnime: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMediaClick(activity.mediaId, activity.isAnime) },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformMediaCover(
            coverUrl = activity.coverUrl,
            palette = emptyList(),
            contentDescription = activity.mediaTitle,
            modifier = Modifier
                .width(PlatformCoverSize.RowPosterWidth / 2)
                .aspectRatio(PlatformCoverSize.PosterAspectRatio),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = buildAnnotatedString {
                    append(activity.statusText)
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(activity.mediaTitle)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TextContent(activity: FeedActivity.Text) {
    val rawText = activity.text
        .replace(Regex("<[^>]+>"), "")
        .trim()
    if (rawText.isNotBlank()) {
        Text(
            text = rawText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageContent(activity: FeedActivity.Message) {
    val rawMessage = activity.message
        .replace(Regex("<[^>]+>"), "")
        .trim()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(activity.messengerName)
                }
                append(" → ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(activity.recipientName)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (rawMessage.isNotBlank()) {
            Text(
                text = rawMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetaCount(icon: @Composable () -> Unit, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon()
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

