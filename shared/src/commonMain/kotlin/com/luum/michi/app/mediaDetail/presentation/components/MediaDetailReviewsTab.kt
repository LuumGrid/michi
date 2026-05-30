package com.luum.michi.app.mediaDetail.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.feed.presentation.components.ReviewCard
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

@Composable
internal fun ReviewsTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val reviews = stateHolder.reviews
    LaunchedEffect(Unit) {
        stateHolder.loadReviews()
    }
    if (reviews.isEmpty() && !stateHolder.isLoadingReviews) {
        PlatformListMessage(
            title = strings.mediaDetailNoReviewsLabel,
            tone = PlatformListMessageTone.Neutral,
        )
        return
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, stateHolder.reviewsHasNextPage) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { last ->
            if (last >= reviews.size - 4 && stateHolder.reviewsHasNextPage && !stateHolder.isLoadingReviews) {
                stateHolder.loadMoreReviews()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(items = reviews, key = { it.id }) { entry ->
            ReviewCard(
                reviewerName = entry.reviewerName,
                reviewerImageUrl = entry.reviewerImageUrl,
                rating = entry.rating,
                summary = entry.summary,
                mediaTitle = null,
                onClick = null,
            )
        }
        if (stateHolder.isLoadingReviews) {
            item { LoadingTile() }
        }
    }
}
