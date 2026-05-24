package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
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
    Column(modifier = Modifier.width(PlatformCoverSize.RailPosterWidth).then(clickModifier)) {
        PlatformMediaCover(
            coverUrl = media.coverUrl,
            palette = media.palette,
            contentDescription = media.title,
            modifier = Modifier
                .width(PlatformCoverSize.RailPosterWidth)
                .aspectRatio(PlatformCoverSize.PosterAspectRatio),
        )
        Text(
            text = media.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
