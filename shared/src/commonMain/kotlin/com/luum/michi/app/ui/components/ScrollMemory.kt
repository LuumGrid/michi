package com.luum.michi.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Scroll rule (app-wide): changing **what** is shown — chip, filter, sort, day,
 * media type, a new destination — returns to the top; refreshing, closing a
 * sheet or coming back (back, another surface) keeps the position.
 *
 * The state is keyed by [resetKey], so a change re-creates it **in the same
 * composition** (the top is painted in the very next frame, no effect tick), and
 * it stays saveable so Root's surface holder restores the position on re-entry
 * (the inputs match then, so it is not re-initialised).
 */
@Composable
internal fun rememberResetListState(resetKey: Any?): LazyListState =
    rememberSaveable(resetKey, saver = LazyListState.Saver) { LazyListState() }

/** [rememberResetListState] for a poster grid (studio, "View all"). */
@Composable
internal fun rememberResetGridState(resetKey: Any?): LazyGridState =
    rememberSaveable(resetKey, saver = LazyGridState.Saver) { LazyGridState() }
