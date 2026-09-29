package com.luum.michi.app.calendar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.model.StreamingPlatform
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.label
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.language.Strings

/**
 * One airing release as a cover card: title + episode/time subtitle, meta
 * line mirroring the list cards (minus the behind/release notes, which
 * belong to lists), plus a streaming platform rail. Card tap navigates
 * to detail (dormant until detail nav lands, same as the list cards);
 * the id is null only for malformed feed items, which no-op instead of
 * navigating nowhere.
 */
@Composable
internal fun CalendarReleaseRow(
    entry: CalendarEntry,
    onOpenDetail: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    val item = entry.item
    val seasonYear = item.season?.let { season ->
        val year = item.seasonYear?.toString()
        if (year != null) "${season.label(strings)} $year" else season.label(strings)
    } ?: item.seasonYear?.toString()
    val meta = listOfNotNull(
        item.format?.label(),
        seasonYear,
        item.mediaStatus?.label(strings, isManga = false),
    ).joinToString(" · ")
    MediaCoverCard(
        coverUrl = item.coverUrl,
        title = item.title,
        subtitle = "${item.release} · ${item.time}",
        onClick = { item.id?.let(onOpenDetail) },
        modifier = modifier,
        content = {
            ReleaseMetaRow(
                meta = meta,
                platforms = item.streamingPlatforms,
                onOpenUrl = onOpenUrl,
            )
        },
    )
}

@Composable
private fun ColumnScope.ReleaseMetaRow(
    meta: String,
    platforms: List<StreamingPlatform>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.weight(1f)) {
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (platforms.isNotEmpty()) {
            // URLs already travel in StreamingPlatform.url; opening a
            // browser needs a platform launcher (deferred), so taps no-op.
            ChipRail(
                tabs = platforms.map { ChipTab(it.url, it.site, iconUrl = it.iconUrl) },
                selected = null,
                onSelect = onOpenUrl,
                contentPadding = PaddingValues(0.dp),
                compact = true,
            )
        }
    }
}
