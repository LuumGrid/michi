package com.luum.michi.app.mediaList.ui.anime.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.behindLabel
import com.luum.michi.app.mediaList.domain.anime.model.canIncrement
import com.luum.michi.app.mediaList.domain.anime.model.formattedScore
import com.luum.michi.app.mediaList.domain.anime.model.progressLabel
import com.luum.michi.app.ui.icons.AppIcons

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
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Behind only: release info already lives in the card subtitle, so
        // falling back to it here would print the date twice.
        val note = entry.behindLabel(strings)
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(modifier = Modifier)
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

/**
 * The user's own rating: filled star + number in a static ghost pill. Only
 * rendered with a real score — "-" adds nothing. Global averages (other
 * surfaces, later) use the outline twin.
 */
@Composable
private fun ScorePill(
    score: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = score,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Counter + increment as two sibling pills: the counter is a static ghost,
 * only +1 acts. The 8dp air between pills is the separator — no divider
 * needed. Card taps open the editor (AL-chan pattern: implicit edit, no
 * button); cover/title taps will navigate to detail.
 * Anime-scoped for now; if the manga twin matches, it extracts to
 * `mediaList/ui/common/` like the rail.
 */
@Composable
private fun ProgressGroup(
    progress: String,
    action: String,
    showAction: Boolean,
    enabled: Boolean,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Text(
                text = progress,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        // +1 only where progress can move (Watching): elsewhere the counter
        // stands alone — showing a dead action would imply it works.
        if (showAction) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onIncrement,
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}
