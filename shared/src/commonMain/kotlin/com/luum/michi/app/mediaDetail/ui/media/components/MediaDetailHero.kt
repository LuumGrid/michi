package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRank
import com.luum.michi.app.mediaDetail.domain.media.model.bestPopularRank
import com.luum.michi.app.mediaDetail.domain.media.model.bestRatedRank
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider
import com.luum.michi.app.ui.theme.brandColor
import com.luum.michi.app.ui.rememberCoverUrl

/**
 * Full-bleed banner (230dp) with a bottom gradient into `surface`. The cover
 * is centered over the banner on both axes; the title/studio/meta live just
 * under the banner. Falls back to the entry's palette color (or
 * `surfaceVariant`) when there is no banner image.
 */
@Composable
internal fun MediaDetailHero(detail: MediaDetail, strings: LanguageStrings) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    val meta = listOfNotNull(detail.format, detail.season, detail.workStatusLabel(strings)).joinToString(" · ")
    val studio = detail.studios.joinToString(", ") { it.name }.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT),
            // Cover dead-center over the backdrop, both axes.
            contentAlignment = Alignment.Center,
        ) {
            BannerBackdrop(
                bannerUrl = detail.bannerUrl,
                paletteHex = detail.paletteHex,
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = if (glass) glassBorder() else solidBorder(),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .width(COVER_WIDTH)
                    .aspectRatio(3f / 4f),
            ) {
                val image = rememberCoverUrl(detail.cover, COVER_WIDTH)
                if (!image.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        loading = {},
                        error = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (studio != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = studio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val BANNER_HEIGHT = 230.dp
private val COVER_WIDTH = 150.dp

@Composable
private fun BannerBackdrop(
    bannerUrl: String?,
    paletteHex: String?,
    modifier: Modifier,
) {
    val fallback = paletteHex?.let { brandColor(it) }?.takeIf { it != Color.Transparent }
        ?: MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.background(fallback)) {
        if (!bannerUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {},
                error = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.surface,
                    ),
                ),
        )
    }
}

private data class HeroStat(val label: String, val value: String, val subtitle: String?)

/**
 * Three equal stat columns (average score, popularity, favourites) with the
 * relevant ranking as their subtitle — highest rated under the score, most
 * popular under popularity. A releasing show leads with the next-episode
 * band. Columns and band are simply omitted when their data is unknown.
 */
@Composable
internal fun MediaDetailHeroStats(detail: MediaDetail, strings: LanguageStrings) {
    val ratedRank = detail.rankings.bestRatedRank()
    val popularRank = detail.rankings.bestPopularRank()
    val columns = buildList {
        detail.averageScore?.let {
            add(
                HeroStat(
                    label = strings.mediaDetailAverageScoreLabel,
                    value = "$it%",
                    subtitle = ratedRank?.let { rank -> rankSubtitle(rank, strings) },
                ),
            )
        }
        detail.popularity?.let {
            add(
                HeroStat(
                    label = strings.popularityLabel,
                    value = compactCount(it),
                    subtitle = popularRank?.let { rank -> rankSubtitle(rank, strings) },
                ),
            )
        }
        detail.favourites?.let { add(HeroStat(strings.favoritesLabel, compactCount(it), null)) }
    }
    // Airing band only while the drop is still ahead of the user.
    val next = detail.nextEpisode?.takeIf { (it.timeUntilAiringSeconds ?: 0L) > 0L }
    if (columns.isEmpty() && next == null) return
    MediaDetailPanel {
        if (next != null) {
            val seconds = next.timeUntilAiringSeconds ?: 0L
            val countdown = strings.airingCountdownLabel(
                days = (seconds / 86_400).toInt(),
                hours = ((seconds % 86_400) / 3_600).toInt(),
                minutes = ((seconds % 3_600) / 60).toInt(),
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.mediaDetailNextEpisodeReleaseLabel(
                        episode = next.episode,
                        releaseDateTime = next.airingAtEpochSeconds.toLocalMediaReleaseDateTime(),
                        countdown = countdown,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            if (columns.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))
        }
        if (columns.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                columns.forEach { stat ->
                    StatColumn(
                        label = stat.label,
                        value = stat.value,
                        subtitle = stat.subtitle,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** `#41 · All time` / `#5 · 2026`. */
private fun rankSubtitle(rank: MediaRank, strings: LanguageStrings): String {
    val scope = if (rank.allTime) strings.mediaDetailAllTimeLabel else rank.year?.toString()
    return listOfNotNull("#${rank.rank}", scope).joinToString(" · ")
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Compact count for stat columns: `950`, `1.8K`, `12K`, `1.3M`. Truncates
 * (never rounds up across a unit) and drops a zero decimal.
 */
private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> compactUnit(value, 1_000_000, "M")
    value >= 1_000 -> compactUnit(value, 1_000, "K")
    else -> value.toString()
}

private fun compactUnit(value: Int, unit: Int, suffix: String): String {
    val tenths = value.toLong() * 10 / unit
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0L || whole >= 10) "$whole$suffix" else "$whole.$fraction$suffix"
}
