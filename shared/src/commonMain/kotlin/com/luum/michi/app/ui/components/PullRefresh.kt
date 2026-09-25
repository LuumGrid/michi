package com.luum.michi.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared pull-to-refresh: M3's floating indicator over the caller's
 * scrollable. Takes only refresh state + callback, so any feature screen
 * (lists today, dashboard/profile tomorrow) reuses it without the holder
 * leaking in. If the inline Instagram-style indicator ever wins, it lands
 * here once — not in N screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState(),
        modifier = modifier,
    ) {
        content()
    }
}
