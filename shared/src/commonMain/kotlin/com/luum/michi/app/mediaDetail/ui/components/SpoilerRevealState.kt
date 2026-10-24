package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Which spoiler ranges of one text are visible. Memory only: the state is
 * created per detail screen and disappears with it, and a new text (a refresh
 * that changed the biography) starts hidden again.
 */
@Stable
internal class SpoilerRevealState {
    private var revealed by mutableStateOf(emptySet<Int>())

    fun isRevealed(index: Int): Boolean = index in revealed

    fun reveal(index: Int) {
        if (index !in revealed) revealed = revealed + index
    }

    /** Blocks can be closed again; inline spoilers only ever reveal. */
    fun toggle(index: Int) {
        revealed = if (index in revealed) revealed - index else revealed + index
    }
}

/**
 * Keyed by detail id + text: ranges are positional, so keeping the state across
 * a text change would reveal the wrong spoiler.
 */
@Composable
internal fun rememberSpoilerRevealState(detailId: Int, text: String): SpoilerRevealState =
    remember(detailId, text) { SpoilerRevealState() }
