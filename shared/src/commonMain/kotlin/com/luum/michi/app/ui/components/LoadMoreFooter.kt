package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Lazy key of the auto-pagination footer: a list/grid observes it to page. */
internal const val LoadMoreKey = "load-more-footer"

/**
 * Full-width spinner footer shared by the auto-paginating lists and grids
 * ("View all", the studio grid). It paints nothing while no page is in flight,
 * but stays composed so its [LoadMoreKey] can be observed: the list loads the
 * next page as soon as the footer scrolls into view.
 */
@Composable
internal fun LoadMoreFooter(isLoading: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}
