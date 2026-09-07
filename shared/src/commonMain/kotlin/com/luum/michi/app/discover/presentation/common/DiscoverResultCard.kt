package com.luum.michi.app.discover.presentation.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.FavouritesBadge
import com.luum.michi.app.ui.components.MediaCover
import com.luum.michi.app.ui.components.RatingBadge
import com.luum.michi.app.discover.domain.model.ExploreResult

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiscoverResultCard(
    result: ExploreResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MediaCover(
            coverUrl = result.coverUrl,
            paletteHex = result.paletteHex,
            contentDescription = result.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f),
        ) {
            if (result.averageScore != null) {
                RatingBadge(averageScore = result.averageScore, isUserRanked = result.isUserRanked)
            }
            if (result.favourites != null && result.favourites > 0) {
                FavouritesBadge(favourites = result.favourites, isUserFavorited = result.isUserFavorited)
            }
        }
        Text(
            text = result.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (result.meta.isNotBlank()) {
            Text(
                text = result.meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (result.genres.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                result.genres.take(3).forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
