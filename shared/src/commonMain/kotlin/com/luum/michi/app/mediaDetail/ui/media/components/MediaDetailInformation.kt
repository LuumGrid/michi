package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaExternalLink
import com.luum.michi.app.mediaDetail.domain.media.model.isOfficialSite
import com.luum.michi.app.mediaDetail.domain.media.model.siteLabel
import com.luum.michi.app.mediaDetail.domain.media.model.videoUrl
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.brandColor

/**
 * Framed information panel: a two-column grid of the short identity fields
 * (type, status, counts, runtime, season, dates, source) with the long ones
 * (titles, synonyms) full width below, then tappable studio chips. Style-
 * aware: glass or solid recipe, same layout in both.
 */
@Composable
internal fun MediaDetailInformationPanel(
    detail: MediaDetail,
    strings: LanguageStrings,
    isManga: Boolean,
    onOpenStudio: (Int) -> Unit,
) {
    val shortRows = buildList {
        detail.format?.let { add(InfoEntry(strings.mediaDetailFormatLabel, it)) }
        detail.workStatusLabel(strings)?.let { add(InfoEntry(strings.mediaDetailStatusLabel, it)) }
        if (isManga) {
            detail.chapters?.let { add(InfoEntry(strings.chaptersLabel, it.toString())) }
            detail.volumes?.let { add(InfoEntry(strings.volumesLabel, it.toString())) }
        } else {
            detail.episodes?.let { add(InfoEntry(strings.mediaDetailEpisodesLabel, it.toString())) }
        }
        detail.duration?.let { add(InfoEntry(strings.mediaDetailDurationLabel, "$it min")) }
        detail.season?.let { add(InfoEntry(strings.mediaDetailSeasonLabel, it)) }
        detail.startedLabel?.let { add(InfoEntry(strings.mediaDetailStartDateLabel, it)) }
        detail.endedLabel?.let { add(InfoEntry(strings.mediaDetailEndedLabel, it)) }
        detail.source?.let { add(InfoEntry(strings.mediaDetailSourceLabel, humanizeSource(it))) }
    }
    val wideRows = buildList {
        detail.titleRomaji?.takeIf { it.isNotBlank() }?.let { add(InfoEntry(strings.mediaDetailRomajiLabel, it)) }
        detail.titleNative?.takeIf { it.isNotBlank() }?.let { add(InfoEntry(strings.mediaDetailNativeLabel, it)) }
        detail.synonyms
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(InfoEntry(strings.mediaDetailSynonymsLabel, it.joinToString(", "))) }
    }
    if (shortRows.isEmpty() && wideRows.isEmpty() && detail.studios.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            shortRows.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    pair.forEach { entry ->
                        InfoCell(
                            label = entry.label,
                            value = entry.value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Odd tail: keep the lone cell half width, not stretched.
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            wideRows.forEach { entry ->
                InfoCell(
                    label = entry.label,
                    value = entry.value,
                    valueMaxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (detail.studios.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.mediaDetailStudioLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            ChipRail(
                tabs = detail.studios.map { ChipTab(value = it.id, label = it.name) },
                selected = null,
                onSelect = onOpenStudio,
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}

private data class InfoEntry(val label: String, val value: String)

/** Label above value: reads cleanly in a two-column grid and full width alike. */
@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 2,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Genres as display-only pills inside a framed panel. */
@Composable
internal fun MediaDetailGenres(detail: MediaDetail, strings: LanguageStrings) {
    if (detail.genres.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailGenresTitle)
        Spacer(modifier = Modifier.height(12.dp))
        ChipRail(
            tabs = detail.genres.map { ChipTab(value = it, label = it) },
            selected = null,
            onSelect = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

/** Non-spoiler tag pills: `"name rank%"`, display-only, inside a framed panel. */
@Composable
internal fun MediaDetailTags(detail: MediaDetail, strings: LanguageStrings) {
    val tags = detail.tags.filter { !it.isMediaSpoiler }
    if (tags.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailTagsTitle)
        Spacer(modifier = Modifier.height(12.dp))
        ChipRail(
            tabs = tags.map { ChipTab(value = it.name, label = "${it.name} ${it.rank}%") },
            selected = null,
            onSelect = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

/** Framed link list: site icon (or brand dot) + site name, tap opens the URL. */
@Composable
internal fun MediaDetailExternalLinks(
    links: List<MediaExternalLink>,
    title: String,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    if (links.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(4.dp))
        links.forEach { link ->
            LinkRow(link = link, strings = strings, onOpenUrl = onOpenUrl)
        }
    }
}

@Composable
private fun LinkRow(
    link: MediaExternalLink,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    val accent = link.color?.let { brandColor(it) }?.takeIf { it != Color.Transparent }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(link.url) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (link.isOfficialSite()) {
            Icon(
                imageVector = AppIcons.Web,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        } else if (!link.iconUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = link.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loading = {},
                error = {},
                modifier = Modifier.size(24.dp),
            )
        } else if (accent != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = link.siteLabel(strings),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        link.language?.takeIf { it.isNotBlank() }?.let { language ->
            Text(
                text = "($language)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Trailer card; taps open the external video URL when there is one. */
@Composable
internal fun MediaDetailVideos(
    detail: MediaDetail,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    val trailer = detail.trailer ?: return
    val url = trailer.videoUrl()
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = strings.mediaDetailVideosTitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (url != null) Modifier.clickable { onOpenUrl(url) } else Modifier),
        ) {
            if (!trailer.thumbnailUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = trailer.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** "LIGHT_NOVEL" → "Light Novel", "ORIGINAL" → "Original". */
private fun humanizeSource(raw: String): String =
    raw.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
