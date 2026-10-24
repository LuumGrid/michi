package com.luum.michi.app.calendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.model.StreamingPlatform
import com.luum.michi.app.calendar.domain.model.formatAiringTime
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.airingCountdown
import com.luum.michi.app.core.media.domain.catalogMetaLine
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.language.Strings
import kotlinx.datetime.TimeZone

/**
 * One airing release as a cover card: title + format/season meta subtitle,
 * episode/time/countdown line in list-card type, plus a streaming platform
 * rail. The episode label comes from strings, the time from [timeZone] and the
 * countdown from [nowEpochSeconds] (both owned by the holder, so no clock read
 * happens in composition). Card tap navigates to detail; the id is null only
 * for malformed feed items, which no-op instead of navigating nowhere.
 */
@Composable
internal fun CalendarReleaseRow(
    entry: CalendarEntry,
    onOpenDetail: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    timeZone: TimeZone,
    nowEpochSeconds: Long,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    val item = entry.item
    val meta = catalogMetaLine(
        format = item.format?.label(strings.formatUnknownLabel),
        season = item.season?.label(strings),
        year = item.seasonYear,
        status = item.mediaStatus?.label(strings, isManga = false),
    )
    val countdown = airingCountdown(strings, item.airingAtEpoch - nowEpochSeconds)
    val episodeLine = buildString {
        append(strings.calendarEpisodeLabel(item.episode))
        append(" - ")
        append(formatAiringTime(item.airingAtEpoch, timeZone))
        if (countdown != null) {
            append(", ")
            append(countdown)
        }
    }
    MediaCoverCard(
        cover = item.cover,
        title = item.title,
        subtitle = meta,
        onClick = { item.id?.let(onOpenDetail) },
        modifier = modifier,
        content = {
            ReleaseMetaRow(
                episodeLine = episodeLine,
                platforms = item.streamingPlatforms,
                onOpenUrl = onOpenUrl,
            )
        },
    )
}

@Composable
private fun ColumnScope.ReleaseMetaRow(
    episodeLine: String,
    platforms: List<StreamingPlatform>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Episode line is bounded; the platform rail is measured first and
        // stays pinned at the cover bottom instead of being pushed out.
        Text(
            text = episodeLine,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (platforms.isNotEmpty()) {
            // URLs already travel in StreamingPlatform.url; a tap opens the
            // platform through the caller's onOpenUrl.
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
