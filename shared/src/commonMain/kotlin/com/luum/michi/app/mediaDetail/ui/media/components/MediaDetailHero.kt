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
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.bestPopularRank
import com.luum.michi.app.mediaDetail.domain.media.model.bestRatedRank
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider
import com.luum.michi.app.ui.theme.brandColor

/** Section header shared by the detail sections (no padding: callers own it). */
@Composable
internal fun MediaDetailSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

/**
 * Full-bleed banner (230dp) with a bottom gradient into `surface`. The cover
 * overlaps the banner's lower edge, centered, and the title/studio/meta live
 * under it. Falls back to the entry's palette color (or `surfaceVariant`)
 * when there is no banner image.
 */
@Composable
internal fun MediaDetailHero(detail: MediaDetail, strings: LanguageStrings) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    val meta = listOfNotNull(detail.format, detail.season, detail.workStatusLabel(strings)).joinToString(" · ")
    val studio = detail.studios.joinToString(", ") { it.name }.takeIf { it.isNotBlank() }
    Box(modifier = Modifier.fillMaxWidth()) {
        BannerBackdrop(
            bannerUrl = detail.bannerUrl,
            paletteHex = detail.paletteHex,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = if (glass) glassBorder() else solidBorder(),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .width(150.dp)
                        .aspectRatio(3f / 4f),
                ) {
                    val image = detail.coverFullUrl ?: detail.coverUrl
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
                Spacer(modifier = Modifier.height(12.dp))
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
            if (detail.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ChipRail(
                    tabs = detail.genres.map { ChipTab(value = it, label = it) },
                    selected = null,
                    onSelect = {},
                )
            }
        }
    }
}

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

/**
 * Radial average-score gauge + three equal stat columns (average, highest
 * rated, most popular). Ranks fall back to an em dash when absent.
 */
@Composable
internal fun MediaDetailHeroStats(detail: MediaDetail, strings: LanguageStrings) {
    val average = detail.averageScore
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                label = strings.mediaDetailAverageScoreLabel,
                value = average?.let { "$it%" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            StatColumn(
                label = strings.mediaDetailHighestRatedLabel,
                value = detail.rankings.bestRatedRank()?.let { "#${it.rank}" } ?: "—",
                subtitle = strings.mediaDetailAllTimeLabel,
                modifier = Modifier.weight(1f),
            )
            StatColumn(
                label = strings.mediaDetailMostPopularLabel,
                value = detail.rankings.bestPopularRank()?.let { "#${it.rank}" } ?: "—",
                subtitle = strings.mediaDetailAllTimeLabel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

