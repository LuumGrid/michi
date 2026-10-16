package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.compactCount
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.upcomingNextEpisode
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider
import com.luum.michi.app.ui.theme.brandColor
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.rememberCoverUrl
import com.luum.michi.app.ui.components.coverSize

/**
 * Full-bleed banner (230dp) with a bottom gradient into `surface`. The cover is
 * centered over the banner on both axes; the title/meta live just under the
 * banner (the studios are chips in the information panel, not a hero line).
 * Falls back to the entry's palette color (or `surfaceVariant`) when there is no
 * banner image.
 */
@Composable
internal fun MediaDetailHero(detail: MediaDetail, strings: LanguageStrings) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    val meta = listOfNotNull(detail.format, detail.season, detail.workStatusLabel(strings)).joinToString(" · ")
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
                modifier = Modifier.coverSize(CoverSize.Featured),
            ) {
                val image = rememberCoverUrl(detail.cover, CoverSize.Featured)
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
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
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

private data class HeroStat(
    val icon: ImageVector,
    val label: String,
    val value: String,
)

/**
 * Three equal stat columns (average score, popularity, favourites) as a
 * frameless full-width row — the same icons the cover badges use. Painted only
 * when at least one column has data (the next-episode band is its own item,
 * [MediaDetailNextEpisodeBand]).
 */
@Composable
internal fun MediaDetailHeroStats(detail: MediaDetail, strings: LanguageStrings) {
    val columns = buildList {
        detail.averageScore?.let {
            add(HeroStat(AppIcons.StarOutline, strings.mediaDetailAverageScoreLabel, "$it%"))
        }
        detail.popularity?.let {
            add(HeroStat(AppIcons.Groups, strings.popularityLabel, it.compactCount()))
        }
        detail.favourites?.let {
            add(HeroStat(AppIcons.Favorite, strings.favoritesLabel, it.compactCount()))
        }
    }
    if (columns.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        columns.forEach { stat ->
            StatColumn(
                icon = stat.icon,
                label = stat.label,
                value = stat.value,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Full-width band with the whole next-episode line (episode + release date/time
 * + countdown). Flat and inert like the informational chips — `surface` +
 * `outlineVariant`, 12dp radius, no ripple — so it does not read as a button;
 * the text wraps and the rectangle grows with it. Painted only while the drop is
 * still ahead of the user.
 */
@Composable
internal fun MediaDetailNextEpisodeBand(detail: MediaDetail, strings: LanguageStrings) {
    val next = detail.upcomingNextEpisode() ?: return
    val seconds = next.timeUntilAiringSeconds ?: 0L
    val countdown = strings.airingCountdownLabel(
        days = (seconds / 86_400).toInt(),
        hours = ((seconds % 86_400) / 3_600).toInt(),
        minutes = ((seconds % 3_600) / 60).toInt(),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

// compactCount (stat columns) now lives in core/model, shared with the rails.
