package com.luum.michi.app.mediaList.ui.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.behindLabel
import com.luum.michi.app.mediaList.domain.anime.model.canIncrement
import com.luum.michi.app.mediaList.domain.anime.model.progressLabel
import com.luum.michi.app.mediaList.domain.anime.model.releaseLabel
import com.luum.michi.app.mediaList.ui.common.EntryScorePill
import com.luum.michi.app.mediaList.ui.common.ProgressGroup

/**
 * Anime entry body for [MediaCoverCard]: the title stays top-anchored (the
 * card pins it); this slot stacks the airing date and the behind note at the
 * top and pins the bottom row (score pill + progress group) to the cover
 * bottom.
 */
@Composable
internal fun ColumnScope.AnimeListEntryContent(
    entry: AnimeListEntry,
    onIncrement: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
    isSmileyFormat: Boolean = false,
    formatSmiley: (Float) -> String = { "" },
    // False while the list is only hydrated: the `+1` is unsafe until confirmed.
    canEdit: Boolean = true,
) {
    // weight(1f), not fillMaxHeight: inside the card's bounded column it
    // takes the remainder, so SpaceBetween pins the bottom row flush with
    // the cover bottom. fillMaxHeight here would fill everything and push
    // the header out.
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Weighted text slot: it can shrink to ellipsis but never pushes the
        // bottom pills, which are measured first (intrinsic) and stay pinned.
        Column(modifier = Modifier.weight(1f, fill = false)) {
            // Airing date on its own line (was the subtitle before the work
            // status arrived); behind below it, never duplicated.
            val date = entry.releaseLabel(strings)
            if (date != null) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val note = entry.behindLabel(strings)
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EntryScorePill(
                score = entry.score,
                isSmileyFormat = isSmileyFormat,
                formatSmiley = formatSmiley,
            )
            ProgressGroup(
                progress = entry.progressLabel(),
                action = strings.listsIncrementEpisodeAction,
                showAction = entry.status == AnimeListSection.WATCHING,
                enabled = entry.canIncrement() && canEdit,
                onIncrement = onIncrement,
            )
        }
    }
}
