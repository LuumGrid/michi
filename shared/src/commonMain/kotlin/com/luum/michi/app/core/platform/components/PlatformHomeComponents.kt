package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.platform.PlatformIcons

data class PlatformHomeShortcut(
    val label: String,
    val icon: Painter,
    val color: Color,
    val onClick: (() -> Unit)? = null,
)

data class PlatformHomeReleaseItem(
    val title: String,
    val release: String,
    val time: String,
    val colors: List<Color>,
    val id: Int? = null,
    val coverUrl: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val popularity: Int? = null,
)

data class PlatformHomeMediaItem(
    val title: String,
    val meta: String,
    val colors: List<Color>,
    val id: Int? = null,
    val coverUrl: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
)

@Composable
fun PlatformHomeHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlatformHomeShortcutRow(items: List<PlatformHomeShortcut>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { item ->
            val columnModifier = if (item.onClick != null) {
                Modifier.weight(1f).clickable(onClick = item.onClick)
            } else {
                Modifier.weight(1f)
            }
            Column(
                modifier = columnModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = item.color.copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(25.dp),
                            tint = item.color,
                        )
                    }
                }
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PlatformHomeSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
fun PlatformHomeReleaseRail(
    title: String,
    items: List<PlatformHomeReleaseItem>,
    onItemClick: ((Int) -> Unit)? = null,
    onItemLongClick: ((Int) -> Unit)? = null,
) {
    PlatformHomeSection(title = title) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { item ->
                PlatformHomeReleaseCard(
                    item = item,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlatformHomeReleaseCard(
    item: PlatformHomeReleaseItem,
    onClick: ((Int) -> Unit)? = null,
    onLongClick: ((Int) -> Unit)? = null,
) {
    val clickModifier = if (item.id != null && (onClick != null || onLongClick != null)) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke(item.id) },
            onLongClick = onLongClick?.let { handler -> { handler(item.id) } },
        )
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .width(172.dp)
            .then(clickModifier),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformHomePoster(
                colors = item.colors,
                coverUrl = item.coverUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(54.dp)
                    .aspectRatio(0.68f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.release,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PlatformCommunityMetaRow(
                    averageScore = item.averageScore,
                    favourites = item.favourites,
                    popularity = item.popularity,
                )
            }
        }
    }
}

@Composable
fun PlatformHomeCommunityCard(
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = PlatformIcons.UserActivity,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.74f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PlatformHomeMediaRail(
    title: String,
    items: List<PlatformHomeMediaItem>,
    onItemClick: ((Int) -> Unit)? = null,
    onItemLongClick: ((Int) -> Unit)? = null,
) {
    PlatformHomeSection(title = title) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { item ->
                PlatformHomeMediaCard(
                    item = item,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlatformHomeMediaCard(
    item: PlatformHomeMediaItem,
    onClick: ((Int) -> Unit)? = null,
    onLongClick: ((Int) -> Unit)? = null,
) {
    val clickModifier = if (item.id != null && (onClick != null || onLongClick != null)) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke(item.id) },
            onLongClick = onLongClick?.let { handler -> { handler(item.id) } },
        )
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .width(116.dp)
            .then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlatformHomePoster(
            colors = item.colors,
            coverUrl = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f),
        ) {
            if (item.averageScore != null) {
                PlatformRatingBadge(averageScore = item.averageScore)
            }
            if (item.favourites != null && item.favourites > 0) {
                PlatformFavouritesBadge(favourites = item.favourites)
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlatformHomePoster(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    contentDescription: String? = null,
    cornerRadius: Dp = 8.dp,
    fallbackIconSize: Dp = 28.dp,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    PlatformMediaCover(
        coverUrl = coverUrl,
        palette = colors,
        contentDescription = contentDescription,
        modifier = modifier,
        cornerRadius = cornerRadius,
        fallbackIcon = PlatformIcons.Home,
        fallbackIconSize = fallbackIconSize,
        overlay = overlay,
    )
}
