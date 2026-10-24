package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.mediaList.domain.common.formattedScore
import com.luum.michi.app.ui.components.StatPill

/**
 * The card's score slot, shared by the anime/manga entry bodies inside their
 * bottom row: the score pill (or the smiley glyph, which is the rating) when
 * there is a real score, a flexible spacer otherwise so the progress group
 * stays pinned right.
 */
@Composable
internal fun RowScope.EntryScorePill(
    score: Double,
    isSmileyFormat: Boolean,
    formatSmiley: (Float) -> String,
) {
    if (score > 0.0) {
        if (isSmileyFormat) {
            StatPill(value = formatSmiley(score.toFloat()), icon = null)
        } else {
            StatPill(value = formattedScore(score))
        }
    } else {
        Spacer(modifier = Modifier.weight(1f))
    }
}
