package com.luum.michi.app.mediaDetail.presentation.media.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.ui.components.Chips
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.FavouritesBadge
import com.luum.michi.app.ui.components.ListMessage
import com.luum.michi.app.ui.components.ListMessageTone
import com.luum.michi.app.ui.components.MediaCover
import com.luum.michi.app.ui.components.RatingBadge
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.presentation.media.state.MediaDetailStateHolder

@Composable
internal fun ConnectionsTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
    onOpenRelation: (Int) -> Unit,
) {
    var selectedScope by rememberSaveable { mutableStateOf("Related") }
    val scopes = remember { listOf("Related", "Recommendations") }

    LaunchedEffect(selectedScope) {
        if (selectedScope == "Recommendations") {
            stateHolder.loadRecommendations()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Chips(
            items = scopes,
            selectedItem = selectedScope,
            onSelect = { selectedScope = it },
            label = { scope ->
                if (scope == "Related") strings.mediaDetailRelationsTitle else strings.mediaDetailTabRecommendations
            },
            useSoftActiveColor = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (selectedScope == "Related") {
                val relations = stateHolder.detail?.relations.orEmpty()
                if (relations.isEmpty()) {
                    ListMessage(
                        title = strings.mediaDetailNoRecommendationsLabel,
                        tone = ListMessageTone.Neutral,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = relations, key = { it.mediaId }) { relation ->
                            MediaDetailRelationCard(
                                relation = relation,
                                strings = strings,
                                onClick = { onOpenRelation(relation.mediaId) },
                            )
                        }
                    }
                }
            } else {
                val recommendations = stateHolder.recommendations
                if (recommendations.isEmpty() && !stateHolder.isLoadingRecommendations) {
                    ListMessage(
                        title = strings.mediaDetailNoRecommendationsLabel,
                        tone = ListMessageTone.Neutral,
                    )
                } else if (stateHolder.isLoadingRecommendations && recommendations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = recommendations, key = { it.id }) { entry ->
                            MediaRecommendationCard(
                                entry = entry,
                                onClick = { onOpenRelation(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MediaRecommendationCard(
    entry: MediaRecommendationEntry,
    onClick: () -> Unit,
) {
    val countLabel = when {
        entry.episodesCount != null && entry.episodesCount > 0 -> "${entry.episodesCount} eps"
        entry.chaptersCount != null && entry.chaptersCount > 0 -> "${entry.chaptersCount} ch"
        entry.volumesCount != null && entry.volumesCount > 0 -> "${entry.volumesCount} vols"
        else -> null
    }
    ConnectionRowCard(
        coverUrl = entry.coverUrl,
        paletteHex = null,
        title = entry.title,
        averageScore = entry.averageScore,
        favourites = entry.favouritesCount,
        topLine = null,
        metaParts = listOfNotNull(entry.format, entry.year?.toString(), countLabel),
        onClick = onClick,
    )
}

@Composable
internal fun ConnectionRowCard(
    coverUrl: String?,
    paletteHex: String?,
    title: String,
    averageScore: Int?,
    favourites: Int?,
    topLine: String?,
    metaParts: List<String>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MediaCover(
            coverUrl = coverUrl,
            paletteHex = paletteHex,
            contentDescription = title,
            modifier = Modifier
                .width(CoverSize.RowPosterWidth)
                .aspectRatio(CoverSize.PosterAspectRatio),
        ) {
            if (averageScore != null && averageScore > 0) {
                RatingBadge(averageScore = averageScore)
            }
            if (favourites != null && favourites > 0) {
                FavouritesBadge(favourites = favourites)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!topLine.isNullOrBlank()) {
                Text(
                    text = topLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
