package com.luum.michi.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Lazy key of the auto-pagination footer: a list/grid observes it to page. */
internal const val LoadMoreKey = "load-more-footer"

/**
 * Auto-pagination observer for a footer carrying [LoadMoreKey]: loads the next
 * page when it scrolls into view. [loadedPage] is the current page of the backing
 * PagedList, so a page that dedupes to nothing still re-triggers while a failed
 * one does not (`distinctUntilChanged`, and a failure never advances the page).
 * [onLoadMore] is read through `rememberUpdatedState`, so a caller changing the
 * lambda never restarts the flow. One overload per lazy state kind.
 */
@Composable
internal fun LoadMoreEffect(
    state: LazyListState,
    enabled: Boolean,
    loadedPage: Int,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    if (enabled) {
        LaunchedEffect(state, loadedPage) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey } }
                .distinctUntilChanged()
                .filter { it }
                .collect { currentOnLoadMore() }
        }
    }
}

@Composable
internal fun LoadMoreEffect(
    state: LazyGridState,
    enabled: Boolean,
    loadedPage: Int,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    if (enabled) {
        LaunchedEffect(state, loadedPage) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey } }
                .distinctUntilChanged()
                .filter { it }
                .collect { currentOnLoadMore() }
        }
    }
}
