package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.rememberCoverUrl
import com.luum.michi.app.ui.theme.brandColor

/**
 * One cover image at a [CoverSize] token: `CoverShape`, the palette (or
 * `surfaceVariant`) placeholder behind the image, an optional [bordered] edge
 * and an [overlay] slot drawn on top. When there is no image (no URL, AniList's
 * default art already mapped to null, or a failed load) it also centers a muted
 * [AppIcons.Image], while a loading image stays a plain fill.
 */
@Composable
internal fun CoverImage(
    cover: CoverSizes?,
    size: CoverSize,
    modifier: Modifier = Modifier,
    placeholderHex: String? = null,
    bordered: Boolean = false,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .coverSize(size)
            .clip(CoverShape)
            .background(placeholderHex?.let { brandColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
            .then(if (bordered) Modifier.border(flatBorder(), CoverShape) else Modifier),
    ) {
        val url = rememberCoverUrl(cover, size)
        if (url.isNullOrBlank()) {
            NoImageIcon(size)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {},
                error = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NoImageIcon(size)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay?.invoke(this)
    }
}

/** Muted picture-frame mark centered over the placeholder. */
@Composable
private fun BoxScope.NoImageIcon(size: CoverSize) {
    Icon(
        imageVector = AppIcons.Image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .align(Alignment.Center)
            .size(minOf(size.width * 0.3f, 32.dp)),
    )
}
