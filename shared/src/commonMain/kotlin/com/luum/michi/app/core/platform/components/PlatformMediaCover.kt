package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.platform.PlatformIcons

/**
 * Canonical poster sizes across the app. Use these whenever rendering a media
 * cover so rails/grids/rows stay visually aligned. Aspect ratio is 0.68 (AniList
 * poster ratio); width drives height implicitly.
 */
object PlatformCoverSize {
    /** Vertical poster in horizontal rails and 2-column grids (Home rails, Explore, Account favorites). */
    val RailPosterWidth: Dp = 116.dp
    /** Horizontal row card cover (Library, Calendar, Detail Connections). */
    val RowPosterWidth: Dp = 93.dp
    /** Aspect ratio used by every PlatformMediaCover. */
    const val PosterAspectRatio: Float = 0.68f
}

@Composable
fun PlatformMediaCover(
    coverUrl: String?,
    palette: List<Color>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    fallbackIcon: Painter? = null,
    fallbackIconSize: Dp = 32.dp,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val finalPalette = if (palette.size >= 2) palette else listOf(
        Color(0xFF312E81),
        Color(0xFFEC4899),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(finalPalette)),
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (fallbackIcon != null) {
            Icon(
                painter = fallbackIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(fallbackIconSize),
            )
        }
        overlay()
    }
}

@Composable
fun BoxScope.PlatformRatingBadge(
    averageScore: Int,
    isUserRanked: Boolean = false,
    alignment: Alignment = Alignment.TopEnd,
    padding: Dp = 6.dp,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
        modifier = Modifier
            .align(alignment)
            .padding(padding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = if (isUserRanked) PlatformIcons.StarFilled else PlatformIcons.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "$averageScore%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun BoxScope.PlatformFavouritesBadge(
    favourites: Int,
    isUserFavorited: Boolean = false,
    alignment: Alignment = Alignment.BottomStart,
    padding: Dp = 6.dp,
) {
    val text = formatCompactCount(favourites)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f),
        modifier = Modifier
            .align(alignment)
            .padding(padding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = if (isUserFavorited) PlatformIcons.LikeFilled else PlatformIcons.Like,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun PlatformCommunityMetaRow(
    averageScore: Int?,
    favourites: Int?,
    popularity: Int? = null,
    isUserRanked: Boolean = false,
    isUserFavorited: Boolean = false,
) {
    val hasScore = averageScore != null && averageScore > 0
    val hasFavs = favourites != null && favourites > 0
    val hasPop = popularity != null && popularity > 0
    if (!hasScore && !hasFavs && !hasPop) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasScore) {
            MetaChip(
                icon = if (isUserRanked) PlatformIcons.StarFilled else PlatformIcons.Star,
                tint = MaterialTheme.colorScheme.primary,
                text = "$averageScore%",
            )
        }
        if (hasFavs) {
            MetaChip(
                icon = if (isUserFavorited) PlatformIcons.LikeFilled else PlatformIcons.Like,
                tint = MaterialTheme.colorScheme.secondary,
                text = formatCompactCount(favourites),
            )
        }
        if (hasPop) {
            MetaChip(
                icon = PlatformIcons.Groups,
                tint = MaterialTheme.colorScheme.tertiary,
                text = formatCompactCount(popularity),
            )
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.painter.Painter,
    tint: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal fun formatCompactCount(value: Int): String {
    if (value < 1000) return value.toString()
    if (value < 1_000_000) {
        val whole = value / 1000
        val tenths = (value % 1000) / 100
        return if (tenths == 0) "${whole}k" else "$whole.${tenths}k"
    }
    val whole = value / 1_000_000
    val tenths = (value % 1_000_000) / 100_000
    return if (tenths == 0) "${whole}M" else "$whole.${tenths}M"
}
