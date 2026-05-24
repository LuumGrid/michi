package com.luum.michi.app.mediaDetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformMediaCover
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailType
import com.luum.michi.app.mediaDetail.presentation.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

@Composable
internal fun MediaDetailScreen(
    mediaId: Int,
    stateHolder: MediaDetailStateHolder,
    onRequestEdit: (Int) -> Unit,
    onOpenRelation: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(mediaId) { stateHolder.load(mediaId) }

    val detail = stateHolder.detail
    when {
        detail != null && detail.id == mediaId -> MediaDetailContent(
            detail = detail,
            strings = strings,
            onEditClick = { onRequestEdit(mediaId) },
            onOpenRelation = onOpenRelation,
        )
        stateHolder.isLoading -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
        stateHolder.error != null -> PlatformListMessage(
            title = strings.mediaDetailErrorLabel,
            subtitle = stateHolder.error,
            tone = PlatformListMessageTone.Error,
        )
        else -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
    }
}

@Composable
private fun MediaDetailContent(
    detail: MediaDetail,
    strings: LanguageStrings,
    onEditClick: () -> Unit,
    onOpenRelation: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { MediaDetailHeader(detail = detail) }
        item { MediaDetailEntryAction(detail = detail, strings = strings, onClick = onEditClick) }
        item { MediaDetailOverviewSection(detail = detail, strings = strings) }
        if (detail.genres.isNotEmpty()) {
            item { MediaDetailGenres(genres = detail.genres, strings = strings) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item { MediaDetailDescription(text = detail.descriptionPlain, strings = strings) }
        }
        if (detail.relations.isNotEmpty()) {
            item {
                MediaDetailRelationsRow(
                    relations = detail.relations,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
            }
        }
        if (detail.studios.isNotEmpty()) {
            item { MediaDetailStudios(studios = detail.studios, strings = strings) }
        }
    }
}

@Composable
private fun MediaDetailEntryAction(
    detail: MediaDetail,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    val label = if (detail.viewerEntry != null) strings.mediaDetailEditEntryAction
    else strings.mediaDetailAddToListAction
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaDetailHeader(detail: MediaDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.verticalGradient(detail.palette)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlatformMediaCover(
                coverUrl = detail.coverUrl,
                palette = detail.palette,
                contentDescription = detail.title,
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(0.68f),
                fallbackIcon = PlatformIcons.Home,
                fallbackIconSize = 34.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    detail.format,
                    detail.season,
                    detail.startedLabel?.takeIf { detail.season == null },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (detail.averageScore != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "${detail.averageScore}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailOverviewSection(detail: MediaDetail, strings: LanguageStrings) {
    val rows = buildList {
        detail.format?.let { add(strings.mediaDetailFormatLabel to it) }
        detail.status?.let { add(strings.mediaDetailStatusLabel to it) }
        when (detail.type) {
            MediaDetailType.MANGA -> {
                detail.chapters?.let { add(strings.mediaDetailChaptersLabel to it.toString()) }
                detail.volumes?.let { add(strings.mediaDetailVolumesLabel to it.toString()) }
            }
            else -> {
                detail.episodes?.let { add(strings.mediaDetailEpisodesLabel to it.toString()) }
                detail.duration?.let { add(strings.mediaDetailDurationLabel to "$it min") }
            }
        }
        detail.season?.let { add(strings.mediaDetailSeasonLabel to it) }
        detail.startedLabel?.let { add(strings.mediaDetailStartedLabel to it) }
        detail.endedLabel?.let { add(strings.mediaDetailEndedLabel to it) }
        detail.source?.let { add(strings.mediaDetailSourceLabel to it) }
        detail.meanScore?.let { add(strings.mediaDetailMeanScoreLabel to "$it%") }
        detail.popularity?.let { add(strings.mediaDetailPopularityLabel to it.toString()) }
        detail.favourites?.let { add(strings.mediaDetailFavoritesLabel to it.toString()) }
    }
    if (rows.isEmpty()) return

    MediaDetailSection(title = strings.mediaDetailOverviewTitle) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        modifier = Modifier.width(140.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaDetailGenres(genres: List<String>, strings: LanguageStrings) {
    MediaDetailSection(title = strings.mediaDetailGenresTitle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunks = genres.chunked(3)
                chunks.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { genre ->
                            AssistChip(
                                onClick = {},
                                label = { Text(genre) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailDescription(text: String, strings: LanguageStrings) {
    var expanded by remember(text) { mutableStateOf(false) }
    val isLong = text.length > 280
    MediaDetailSection(title = strings.mediaDetailDescriptionTitle) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded || !isLong) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLong) {
                Text(
                    text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
        }
    }
}

@Composable
private fun MediaDetailRelationsRow(
    relations: List<MediaDetailRelation>,
    strings: LanguageStrings,
    onOpenRelation: (Int) -> Unit,
) {
    MediaDetailSection(title = strings.mediaDetailRelationsTitle) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
}

@Composable
private fun MediaDetailRelationCard(
    relation: MediaDetailRelation,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(108.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlatformMediaCover(
            coverUrl = relation.coverUrl,
            palette = relation.palette,
            contentDescription = relation.title,
            modifier = Modifier
                .width(108.dp)
                .aspectRatio(0.68f),
        )
        Text(
            text = relationLabel(relation.kind, strings),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = relation.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun relationLabel(kind: MediaRelationKind, strings: LanguageStrings): String = when (kind) {
    MediaRelationKind.SEQUEL -> strings.mediaRelationSequel
    MediaRelationKind.PREQUEL -> strings.mediaRelationPrequel
    MediaRelationKind.SIDE_STORY -> strings.mediaRelationSideStory
    MediaRelationKind.SPIN_OFF -> strings.mediaRelationSpinOff
    MediaRelationKind.PARENT -> strings.mediaRelationParent
    MediaRelationKind.ADAPTATION -> strings.mediaRelationAdaptation
    MediaRelationKind.OTHER -> strings.mediaRelationOther
}

@Composable
private fun MediaDetailStudios(studios: List<String>, strings: LanguageStrings) {
    MediaDetailSection(title = strings.mediaDetailStudiosTitle) {
        Text(
            text = studios.joinToString(" · "),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MediaDetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}
