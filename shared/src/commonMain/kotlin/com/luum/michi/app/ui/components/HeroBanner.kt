package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.ui.theme.brandColor

/**
 * The banner backdrop shared by the media-detail and profile heroes: the
 * entry's palette color (or `surfaceVariant`) behind an optional banner image
 * with [ContentScale.Crop] and a bottom gradient into `surface`. A null or blank
 * [bannerUrl] keeps only the colored block, so a banner-less entry still reads
 * as a hero instead of an empty gap.
 */
@Composable
internal fun HeroBanner(
    bannerUrl: String?,
    paletteHex: String?,
    modifier: Modifier = Modifier,
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

/** Height of the heroes' banner: the media detail and the profile. */
internal val HeroBannerHeight = 180.dp
