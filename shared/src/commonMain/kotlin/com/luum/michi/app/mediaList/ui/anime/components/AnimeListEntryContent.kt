package com.luum.michi.app.mediaList.ui.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.behindLabel
import com.luum.michi.app.mediaList.domain.anime.model.canIncrement
import com.luum.michi.app.mediaList.domain.anime.model.formattedScore
import com.luum.michi.app.mediaList.domain.anime.model.progressLabel
import com.luum.michi.app.mediaList.domain.anime.model.releaseLabel
import com.luum.michi.app.mediaList.ui.common.ProgressGroup
import com.luum.michi.app.mediaList.ui.common.ScorePill

/**
 * Anime entry body for [MediaCoverCard]: title and note stay top-anchored
 * (the card pins them), this slot owns the vertical space between and pins
 * the progress group to the bottom. Score stays out: it surfaces in the
 * entry editor, not in the rail card.
 */
@Composable
internal fun ColumnScope.AnimeListEntryContent(
    entry: AnimeListEntry,
    onIncrement: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    // weight(1f), not fillMaxHeight: inside the card's bounded column it
    // takes the remainder, so SpaceBetween pins the bottom row flush with
    // the cover bottom. fillMaxHeight here would fill everything and push
    // the header out.
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            // Airing date on its own line (was the subtitle before the work
            // status arrived); behind below it, never duplicated.
            val date = entry.releaseLabel(strings)
            if (date != null) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val note = entry.behindLabel(strings)
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (entry.score > 0.0) {
                ScorePill(score = entry.formattedScore())
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            ProgressGroup(
                progress = entry.progressLabel(),
                action = strings.listsIncrementEpisodeAction,
                showAction = entry.status == AnimeListSection.WATCHING,
                enabled = entry.canIncrement(),
                onIncrement = onIncrement,
            )
        }
    }
}
