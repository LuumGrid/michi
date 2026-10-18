package com.luum.michi.app.notifications.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.notifications.ui.model.NotificationRowItem
import com.luum.michi.app.ui.components.Avatar
import com.luum.michi.app.ui.components.CoverShape
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.rememberCoverUrl

private val ThumbnailSize = 48.dp

/**
 * One notification row: a media cover or an actor avatar, the category text and
 * the relative time. Plain list row (settings-like), tappable: media opens its
 * detail, everything else the browser (the caller decides via [onClick]).
 */
@Composable
internal fun NotificationRow(
    item: NotificationRowItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotificationThumbnail(item)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Actor avatars are circles; media covers are rounded like every other cover. */
@Composable
private fun NotificationThumbnail(item: NotificationRowItem) {
    val url = rememberCoverUrl(item.image, CoverSize.List)
    if (item.isPerson) {
        Avatar(imageUrl = url, initials = item.initials, size = ThumbnailSize)
    } else {
        Box(
            modifier = Modifier
                .size(ThumbnailSize)
                .clip(CoverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (url != null) {
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
