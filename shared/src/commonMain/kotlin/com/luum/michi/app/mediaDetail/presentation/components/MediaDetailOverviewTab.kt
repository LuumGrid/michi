package com.luum.michi.app.mediaDetail.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailType
import com.luum.michi.app.mediaDetail.presentation.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.presentation.model.StudioRef

@Composable
internal fun OverviewTab(
    detail: MediaDetail,
    strings: LanguageStrings,
    onOpenRelation: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { MediaDetailOverviewSection(detail = detail, strings = strings) }
        if (detail.genres.isNotEmpty()) {
            item { MediaDetailGenres(genres = detail.genres, strings = strings) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item { MediaDetailDescription(text = detail.descriptionPlain, strings = strings) }
        }
        if (detail.studios.isNotEmpty()) {
            item {
                MediaDetailStudios(
                    studios = detail.studios,
                    strings = strings,
                    onOpenStudio = onOpenStudio,
                )
            }
        }
    }
}

@Composable
internal fun MediaDetailOverviewSection(detail: MediaDetail, strings: LanguageStrings) {
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
internal fun MediaDetailGenres(genres: List<String>, strings: LanguageStrings) {
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
internal fun MediaDetailDescription(text: String, strings: LanguageStrings) {
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
internal fun MediaDetailRelationCard(
    relation: MediaDetailRelation,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    ConnectionRowCard(
        coverUrl = relation.coverUrl,
        palette = relation.palette,
        title = relation.title,
        averageScore = relation.averageScore,
        favourites = relation.favourites,
        topLine = relationLabel(relation.kind, strings),
        metaParts = listOfNotNull(relation.format, relation.year?.toString()),
        onClick = onClick,
    )
}

internal fun relationLabel(kind: MediaRelationKind, strings: LanguageStrings): String = when (kind) {
    MediaRelationKind.SEQUEL -> strings.mediaRelationSequel
    MediaRelationKind.PREQUEL -> strings.mediaRelationPrequel
    MediaRelationKind.SIDE_STORY -> strings.mediaRelationSideStory
    MediaRelationKind.SPIN_OFF -> strings.mediaRelationSpinOff
    MediaRelationKind.PARENT -> strings.mediaRelationParent
    MediaRelationKind.ADAPTATION -> strings.mediaRelationAdaptation
    MediaRelationKind.ALTERNATIVE -> strings.mediaRelationAlternative
    MediaRelationKind.SOURCE -> strings.mediaRelationSource
    MediaRelationKind.SUMMARY -> strings.mediaRelationSummary
    MediaRelationKind.CHARACTER -> strings.mediaRelationCharacter
    MediaRelationKind.OTHER -> strings.mediaRelationOther
}

@Composable
internal fun MediaDetailStudios(
    studios: List<StudioRef>,
    strings: LanguageStrings,
    onOpenStudio: (Int) -> Unit = {},
) {
    MediaDetailSection(title = strings.mediaDetailStudiosTitle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            studios.forEachIndexed { index, studio ->
                Text(
                    text = studio.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onOpenStudio(studio.id) },
                )
                if (index < studios.lastIndex) {
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MediaDetailSection(
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
