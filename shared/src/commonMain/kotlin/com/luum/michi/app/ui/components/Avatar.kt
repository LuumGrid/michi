package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Circular user/media avatar over Coil 3. [imageUrl] blank (or a failed load)
 * falls back to the [initials] mark, so offline and error states keep the
 * same 48dp-style footprint with zero caller branching. The fallback is the
 * same look the session card had before images existed.
 */
@Composable
internal fun Avatar(
    imageUrl: String?,
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (imageUrl.isNullOrBlank()) {
        InitialsAvatar(initials = initials, size = size, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            loading = {
                InitialsAvatar(initials = initials, size = size)
            },
            error = {
                InitialsAvatar(initials = initials, size = size)
            },
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun InitialsAvatar(
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
