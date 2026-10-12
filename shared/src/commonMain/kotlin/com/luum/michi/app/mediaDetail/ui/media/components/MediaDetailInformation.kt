package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.luum.michi.app.mediaDetail.domain.media.model.isOfficialSite
import com.luum.michi.app.mediaDetail.domain.media.model.siteLabel
import com.luum.michi.app.mediaDetail.domain.media.model.videoUrl
import com.luum.michi.app.mediaDetail.domain.media.model.workStatusLabel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailSectionHeader
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.glassContainerColor
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.components.solidContainerColor
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider
import com.luum.michi.app.ui.theme.brandColor

/**
 * Framed information panel: key/value rows for the identity fields that do
 * not already live in the hero (romaji/native titles, synonyms, format,
 * status, counts, duration, season, studios, dates, source). Style-aware:
 * glass or solid recipe, same layout in both.
 */
@Composable
internal fun MediaDetailInformationPanel(
    detail: MediaDetail,
    strings: LanguageStrings,
    isManga: Boolean,
    onOpenStudio: (Int) -> Unit,
) {
    val rows = buildList {
        detail.titleRomaji?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailRomajiLabel to it) }
        detail.titleNative?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailJapaneseLabel to it) }
        detail.synonyms
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(strings.mediaDetailSynonymsLabel to it.joinToString(", ")) }
        detail.format?.let { add(strings.mediaDetailFormatLabel to it) }
        detail.workStatusLabel(strings)?.let { add(strings.mediaDetailStatusLabel to it) }
        if (isManga) {
            detail.chapters?.let { add(strings.chaptersLabel to it.toString()) }
            detail.volumes?.let { add(strings.volumesLabel to it.toString()) }
        } else {
            detail.episodes?.let { add(strings.mediaDetailEpisodesLabel to it.toString()) }
        }
        detail.duration?.let { add(strings.mediaDetailDurationLabel to "$it min") }
        detail.season?.let { add(strings.mediaDetailSeasonLabel to it) }
        detail.startedLabel?.let { add(strings.mediaDetailStartDateLabel to it) }
        detail.endedLabel?.let { add(strings.mediaDetailEndedLabel to it) }
        detail.source?.let { add(strings.mediaDetailSourceLabel to humanizeSource(it)) }
    }
    if (rows.isEmpty() && detail.studios.isEmpty()) return
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (glass) glassContainerColor() else solidContainerColor(),
            border = if (glass) glassBorder() else solidBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                MediaDetailSectionHeader(strings.mediaDetailInformationTitle)
                Spacer(modifier = Modifier.height(8.dp))
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (detail.studios.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.mediaDetailStudioLabel,
                        style = MaterialTheme.typography.bodyMedium,
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
    }
}

/** Non-spoiler tag chips: `"name rank%"`, display-only. */
@Composable
internal fun MediaDetailTags(detail: MediaDetail, strings: LanguageStrings) {
    val tags = detail.tags.filter { !it.isMediaSpoiler }
    if (tags.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailTagsTitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ChipRail(
            tabs = tags.map { ChipTab(value = it.name, label = "${it.name} ${it.rank}%") },
            selected = null,
            onSelect = {},
        )
    }
}

/** External link rows: site icon (or brand dot) + site name, tap opens the URL. */
@Composable
internal fun MediaDetailExternalLinks(
    detail: MediaDetail,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    if (detail.externalLinks.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailExternalLinksTitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        detail.externalLinks.forEach { link ->
            val accent = link.color?.let { brandColor(it) }?.takeIf { it != Color.Transparent }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl(link.url) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                if (accent != null && !link.iconUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
            }
        }
    }
}

/** Trailer thumbnail card; taps open the external video URL when there is one. */
@Composable
internal fun MediaDetailVideos(
    detail: MediaDetail,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    val trailer = detail.trailer ?: return
    val url = trailer.videoUrl()
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
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
