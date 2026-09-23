package com.luum.michi.app.mediaList.ui.manga.components

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
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.behindLabel
import com.luum.michi.app.mediaList.domain.manga.model.canIncrementChapters
import com.luum.michi.app.mediaList.domain.manga.model.canIncrementVolumes
import com.luum.michi.app.mediaList.domain.manga.model.chaptersProgressLabel
import com.luum.michi.app.mediaList.domain.manga.model.formattedScore
import com.luum.michi.app.mediaList.domain.manga.model.volumesProgressLabel
import com.luum.michi.app.mediaList.ui.common.ProgressGroup
import com.luum.michi.app.mediaList.ui.common.ScorePill

/**
 * Manga entry body for [MediaCoverCard]: mirrors the anime body. One capsule
 * always — chapters normally, volumes for novels ([MangaListEntry.tracksByVolume]).
 * Score only with a real score; behind only (release info has no manga
 * equivalent on this card).
 */
@Composable
internal fun ColumnScope.MangaListEntryContent(
    entry: MangaListEntry,
    onIncrementChapters: () -> Unit,
    onIncrementVolumes: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Behind full width, like anime: sharing the row with the group
        // squeezes it ("232 chapters be…").
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
            if (entry.tracksByVolume) {
                ProgressGroup(
                    progress = entry.volumesProgressLabel(),
                    action = strings.listsIncrementVolumeAction,
                    showAction = entry.status == MangaListSection.CURRENT,
                    enabled = entry.canIncrementVolumes(),
                    onIncrement = onIncrementVolumes,
                )
            } else {
                ProgressGroup(
                    progress = entry.chaptersProgressLabel(),
                    action = strings.listsIncrementChapterAction,
                    showAction = entry.status == MangaListSection.CURRENT,
                    enabled = entry.canIncrementChapters(),
                    onIncrement = onIncrementChapters,
                )
            }
        }
    }
}
