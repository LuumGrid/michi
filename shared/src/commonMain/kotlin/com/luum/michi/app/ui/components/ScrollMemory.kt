package com.luum.michi.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Scroll rule (app-wide): changing **what** is shown — chip, filter, sort, day,
 * media type, a new destination — returns to the top; refreshing, closing a
 * sheet or coming back (back, another surface) keeps the position.
 *
 * The returned state is the saveable `rememberLazyListState()`, so Root's
 * surface holder preserves it across a surface switch (tab ↔ detail); the reset
 * only fires when [resetKey] actually changes, never on re-entry/restore.
 */
@Composable
internal fun rememberResetListState(resetKey: Any?): LazyListState {
    val state = rememberLazyListState()
    ResetScrollOnChange(state, resetKey)
    return state
}

/** [rememberResetListState] for a 2-column grid (studio). */
@Composable
internal fun rememberResetGridState(resetKey: Any?): LazyGridState {
    val state = rememberLazyGridState()
    ResetGridScrollOnChange(state, resetKey)
    return state
}

/** Returns a list to the top when [resetKey] changes (never on re-entry). */
@Composable
internal fun ResetScrollOnChange(state: LazyListState, resetKey: Any?) {
    var previousKey by remember { mutableStateOf(resetKey) }
    LaunchedEffect(resetKey) {
        if (resetKey != previousKey) {
            previousKey = resetKey
            state.scrollToItem(0)
        }
    }
}

/** Returns a grid to the top when [resetKey] changes (never on re-entry). */
@Composable
internal fun ResetGridScrollOnChange(state: LazyGridState, resetKey: Any?) {
    var previousKey by remember { mutableStateOf(resetKey) }
    LaunchedEffect(resetKey) {
        if (resetKey != previousKey) {
            previousKey = resetKey
            state.scrollToItem(0)
        }
    }
}
