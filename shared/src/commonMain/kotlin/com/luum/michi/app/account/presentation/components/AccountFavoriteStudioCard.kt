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
import com.luum.michi.app.account.domain.model.AccountFavoriteStudio
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.MediaCover

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AccountFavoriteStudioCard(
    studio: AccountFavoriteStudio,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier.combinedClickable(onClick = { onClick() })
    } else {
        Modifier
    }
    Column(
        modifier = Modifier.width(CoverSize.RailPosterWidth).then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaCover(
            coverUrl = studio.coverUrl,
            paletteHex = studio.paletteHex,
            contentDescription = studio.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CoverSize.PosterAspectRatio),
        )
        Text(
            text = studio.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
