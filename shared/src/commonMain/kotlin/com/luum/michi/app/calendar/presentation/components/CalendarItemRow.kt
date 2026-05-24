package com.luum.michi.app.calendar.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.components.PlatformCommunityMetaRow
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformHomePoster
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.core.platform.components.StreamingPlatform

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarItemRow(
    item: PlatformHomeReleaseItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformHomePoster(
                colors = item.colors,
                coverUrl = item.coverUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(PlatformCoverSize.RowPosterWidth)
                    .aspectRatio(PlatformCoverSize.PosterAspectRatio),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.release,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                if (item.userStatus != null) {
                    UserStatusPill(status = item.userStatus, strings = LanguageProvider.strings)
                }
                PlatformCommunityMetaRow(
                    averageScore = item.averageScore,
                    favourites = item.favourites,
                    popularity = item.popularity,
                    isUserRanked = item.isUserRanked,
                    isUserFavorited = item.isUserFavorited,
                )
                if (item.streamingPlatforms.isNotEmpty()) {
                    StreamingPlatformChips(platforms = item.streamingPlatforms)
                }
            }
            Text(
                text = item.time,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun UserStatusPill(status: String, strings: LanguageStrings) {
    val label = when (status.uppercase()) {
        "CURRENT" -> strings.mediaStatsStatusCurrent
        "PLANNING" -> strings.mediaStatsStatusPlanning
        "COMPLETED" -> strings.mediaStatsStatusCompleted
        "DROPPED" -> strings.mediaStatsStatusDropped
        "PAUSED" -> strings.mediaStatsStatusPaused
        "REPEATING" -> strings.mediaStatsStatusRepeating
        else -> return
    }
    val color = when (status.uppercase()) {
        "CURRENT", "REPEATING" -> MaterialTheme.colorScheme.primary
        "COMPLETED" -> MaterialTheme.colorScheme.secondary
        "PLANNING" -> MaterialTheme.colorScheme.tertiary
        "DROPPED" -> MaterialTheme.colorScheme.error
        "PAUSED" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StreamingPlatformChips(platforms: List<StreamingPlatform>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = platforms, key = { "${it.site}_${it.url}" }) { platform ->
            StreamingPlatformChip(platform = platform)
        }
    }
}

@Composable
private fun StreamingPlatformChip(platform: StreamingPlatform) {
    val brandColor = parseHexColor(platform.color)
    val container = brandColor ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (brandColor != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = container,
    ) {
        Text(
            text = platform.site,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val normalized = if (hex.startsWith("#")) hex.removePrefix("#") else hex
    return runCatching {
        val value = normalized.toLong(16)
        when (normalized.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> null
        }
    }.getOrNull()
}
