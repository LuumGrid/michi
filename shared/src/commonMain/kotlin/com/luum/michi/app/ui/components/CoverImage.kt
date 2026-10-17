package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.rememberCoverUrl
import com.luum.michi.app.ui.theme.brandColor

/**
 * One cover image at a [CoverSize] token: `CoverShape`, the palette (or
 * `surfaceVariant`) placeholder behind the image, an optional [bordered] edge
 * and an [overlay] slot drawn on top (badges). The placeholder fills while the
 * image loads and if it fails, so callers need no loading/error slots.
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
        if (!url.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {},
                error = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay?.invoke(this)
    }
}
