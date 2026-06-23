package com.luum.michi.app.notifications.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.media.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.notifications.presentation.model.AppNotification
import com.luum.michi.app.notifications.presentation.model.NotificationCategory

@Composable
internal fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformMediaCover(
                coverUrl = notification.imageUrl,
                palette = emptyList(),
                contentDescription = notification.mediaTitle ?: notification.userName,
                modifier = Modifier
                    .width(PlatformCoverSize.RowPosterWidth)
                    .aspectRatio(PlatformCoverSize.PosterAspectRatio),
                cornerRadius = 12.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = notification.describe(strings),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = strings.notificationDateLabel(
                        notification.createdAtEpochSeconds.toLocalMediaReleaseDateTime(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun AppNotification.describe(strings: LanguageStrings): String {
    val title = mediaTitle.orEmpty()
    val name = userName.orEmpty()
    return when (category) {
        NotificationCategory.AIRING -> {
            val episodeText = episode?.toString().orEmpty()
            "${strings.notifAiringPrefix} $episodeText ${strings.notifAiringAired.lowercase()} $title".trim()
        }
        NotificationCategory.FOLLOWING -> "$name ${strings.notifFollowingLabel}".trim()
        NotificationCategory.ACTIVITY -> if (name.isNotBlank()) "$name • ${strings.notifActivityLabel}" else strings.notifActivityLabel
        NotificationCategory.FORUM -> strings.notifForumLabel
        NotificationCategory.MESSAGE -> if (name.isNotBlank()) "$name • ${strings.notifMessageLabel}" else strings.notifMessageLabel
        NotificationCategory.MEDIA_CHANGE -> if (title.isNotBlank()) "$title — ${strings.notifMediaChangeLabel}" else strings.notifMediaChangeLabel
        NotificationCategory.OTHER -> title.ifBlank { name }
    }
}
