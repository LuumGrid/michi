package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.model.AccountFavoriteMedia
import com.luum.michi.app.core.platform.components.PlatformCoverSize
import com.luum.michi.app.core.platform.components.PlatformMediaCover

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AccountFavoriteMediaCard(
    media: AccountFavoriteMedia,
    modifier: Modifier = Modifier.width(PlatformCoverSize.RailPosterWidth),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier.then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlatformMediaCover(
            coverUrl = media.coverUrl,
            palette = media.palette,
            contentDescription = media.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PlatformCoverSize.PosterAspectRatio),
        )
        Text(
            text = media.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
