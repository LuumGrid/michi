package com.luum.michi.app.mediaDetail.ui.media.components

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
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.compactCount
import com.luum.michi.app.ui.components.CoverImage
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.HeroCoverOverlap
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailHeroIdentity
import com.luum.michi.app.core.media.domain.airingCountdown
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.upcomingNextEpisode
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.flatBorder
import com.luum.michi.app.ui.theme.brandColor
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Full-bleed hero: a [MediaDetailBannerHeight] backdrop with a bottom gradient
 * into `surface`, the [CoverSize.Featured] cover overlapping its bottom edge by
 * [HeroCoverOverlap], and the title/meta identity to the right of the cover
 * (the studios are chips in the information panel, not a hero line). Padding —
 * not offset — places the cover, so the Box grows with whatever hangs lowest
 * (a long title can push it past the banner). Falls back to the entry's palette
 * color (or `surfaceVariant`) when there is no banner image.
 */
@Composable
internal fun MediaDetailHero(detail: MediaDetail, strings: LanguageStrings) {
    val meta = listOfNotNull(detail.format, detail.season, detail.workStatusLabel(strings)).joinToString(" · ")
    Box(modifier = Modifier.fillMaxWidth()) {
        BannerBackdrop(
            bannerUrl = detail.bannerUrl,
            paletteHex = detail.paletteHex,
            modifier = Modifier
                .fillMaxWidth()
                .height(MediaDetailBannerHeight),
        )
        CoverImage(
            cover = detail.cover,
            size = CoverSize.Featured,
            bordered = true,
            modifier = Modifier.padding(
                start = 16.dp,
                top = MediaDetailBannerHeight - HeroCoverOverlap,
            ),
        )
        MediaDetailHeroIdentity(
            name = detail.title,
            secondaryLine = meta,
            favourites = null,
            strings = strings,
            chips = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp + CoverSize.Featured.width + 16.dp,
                    end = 16.dp,
                    top = MediaDetailBannerHeight + 12.dp,
                ),
        )
    }
}

private val MediaDetailBannerHeight = 180.dp

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
    val countdown = airingCountdown(strings, next.timeUntilAiringSeconds ?: 0L) ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = PanelShape,
            border = flatBorder(),
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
