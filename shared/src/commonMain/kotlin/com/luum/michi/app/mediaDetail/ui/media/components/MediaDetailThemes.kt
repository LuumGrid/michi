package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme
import com.luum.michi.app.mediaDetail.domain.media.model.MusicService
import com.luum.michi.app.mediaDetail.domain.media.model.searchQuery
import com.luum.michi.app.mediaDetail.domain.media.model.videoSearchUrl
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.ui.components.ChipPill
import com.luum.michi.app.ui.components.PillShape
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.TrailingPill
import com.luum.michi.app.ui.components.flatBorder
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.brandColor

/**
 * One framed panel of AnimeThemes songs (openings or endings): rows that open a
 * chip row of music-service searches + a YouTube video search + the ad-free
 * AnimeThemes page. Three rows; when there are more, a header pill toggles
 * "Show all N" / "Show less".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediaDetailThemePanel(
    title: String,
    themes: List<AnimeTheme>,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    MediaDetailPanel {
        var showAll by remember(themes) { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = title, modifier = Modifier.weight(1f))
            if (themes.size > ThemePreviewRows) {
                TrailingPill(
                    label = if (showAll) {
                        strings.mediaDetailThemesShowLessAction
                    } else {
                        strings.mediaDetailThemesShowAllLabel(themes.size)
                    },
                    onClick = { showAll = !showAll },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Labels of the rows whose chip row is open.
        var open by remember(themes) { mutableStateOf(emptySet<String>()) }
        val visible = if (showAll) themes else themes.take(ThemePreviewRows)
        visible.forEachIndexed { index, theme ->
            ThemeRow(
                theme = theme,
                isOpen = theme.label in open,
                divider = index != 0,
                strings = strings,
                onOpenUrl = onOpenUrl,
                onToggle = {
                    open = if (theme.label in open) {
                        open - theme.label
                    } else {
                        open + theme.label
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeRow(
    theme: AnimeTheme,
    isOpen: Boolean,
    divider: Boolean,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
    onToggle: () -> Unit,
) {
    if (divider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .border(flatBorder(), PillShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                theme.artists.joinToString(", ").takeIf { it.isNotEmpty() },
                theme.episodes?.let { strings.mediaDetailThemeEpisodes(it) },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isOpen) AppIcons.ExpandLess else AppIcons.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    if (isOpen) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusicService.entries.forEach { service ->
                ChipPill(
                    label = service.label,
                    selected = false,
                    compact = true,
                    leadingIcon = service.icon(),
                    leadingIconTint = brandColor(service.brandHex),
                    onClick = { onOpenUrl(service.searchUrl(theme.searchQuery())) },
                )
            }
            ChipPill(
                label = "YouTube",
                selected = false,
                compact = true,
                leadingIcon = AppIcons.YouTube,
                leadingIconTint = brandColor("#FF0000"),
                onClick = { onOpenUrl(theme.videoSearchUrl()) },
            )
            ChipPill(
                label = "AnimeThemes",
                selected = false,
                compact = true,
                leadingIcon = AppIcons.Play,
                onClick = { onOpenUrl(theme.pageUrl) },
            )
        }
    }
}

/** Number of rows shown until the header pill expands the panel. */
private const val ThemePreviewRows = 3

private fun MusicService.icon(): ImageVector = when (this) {
    MusicService.SPOTIFY -> AppIcons.Spotify
    MusicService.YOUTUBE_MUSIC -> AppIcons.YouTubeMusic
    MusicService.APPLE_MUSIC -> AppIcons.AppleMusic
}
