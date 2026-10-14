package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.brandColor

/** One row of a [MediaDetailLinkSection]: label + url (+ optional icon/accent). */
internal data class MediaDetailLinkItem(
    val label: String,
    val url: String,
    val iconUrl: String? = null,
    val colorHex: String? = null,
    val isOfficialSite: Boolean = false,
    val detail: String? = null,
)

/**
 * Framed link list shared by the media detail (streaming / external) and the
 * person pages (biography links): site icon (or brand dot) + label, tap opens
 * the URL. Takes primitives so no scope owns the others' models.
 */
@Composable
internal fun MediaDetailLinkSection(
    links: List<MediaDetailLinkItem>,
    title: String,
    onOpenUrl: (String) -> Unit,
) {
    if (links.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(4.dp))
        links.forEach { item -> MediaDetailLinkRow(item = item, onOpenUrl = onOpenUrl) }
    }
}

@Composable
private fun MediaDetailLinkRow(
    item: MediaDetailLinkItem,
    onOpenUrl: (String) -> Unit,
) {
    val accent = item.colorHex?.let { brandColor(it) }?.takeIf { it != Color.Transparent }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(item.url) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isOfficialSite) {
            Icon(
                imageVector = AppIcons.Web,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        } else if (!item.iconUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loading = {},
                error = {},
                modifier = Modifier.size(24.dp),
            )
        } else if (accent != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        item.detail?.takeIf { it.isNotBlank() }?.let { language ->
            Text(
                text = "($language)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
