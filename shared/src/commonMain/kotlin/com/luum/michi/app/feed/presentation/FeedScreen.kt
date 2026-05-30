package com.luum.michi.app.feed.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.auth.currentEpochSeconds
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.feed.data.FeedChip
import com.luum.michi.app.feed.data.FeedSection
import com.luum.michi.app.feed.presentation.components.FeedActivityCard
import com.luum.michi.app.feed.presentation.components.ReviewCard
import com.luum.michi.app.feed.presentation.state.FeedStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreen(
    stateHolder: FeedStateHolder,
    onMediaClick: (mediaId: Int, isAnime: Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings
    val nowEpochSeconds = remember { currentEpochSeconds() }
    val activityListState = rememberLazyListState()
    val reviewsListState = rememberLazyListState()

    val activityNearEnd by remember {
        derivedStateOf {
            val info = activityListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4
        }
    }

    val reviewsNearEnd by remember {
        derivedStateOf {
            val info = reviewsListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(activityNearEnd) {
        if (activityNearEnd) stateHolder.loadMore()
    }

    LaunchedEffect(reviewsNearEnd) {
        if (reviewsNearEnd) stateHolder.loadMoreReviews()
    }

    LaunchedEffect(Unit) {
        stateHolder.load()
    }

    LaunchedEffect(stateHolder.filter) {
        activityListState.scrollToItem(0)
    }

    LaunchedEffect(stateHolder.section) {
        if (stateHolder.section == FeedSection.REVIEWS && stateHolder.reviews.isEmpty()) {
            stateHolder.loadReviews()
        }
    }

    PullToRefreshBox(
        isRefreshing = if (stateHolder.section == FeedSection.ACTIVITY) {
            stateHolder.isRefreshing
        } else {
            stateHolder.isLoadingReviews
        },
        onRefresh = {
            if (stateHolder.section == FeedSection.ACTIVITY) {
                stateHolder.load(forceRefresh = true)
            } else {
                stateHolder.loadReviews(forceRefresh = true)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlatformChips(
                items = listOf(FeedChip.FOLLOWING, FeedChip.GLOBAL, FeedChip.REVIEWS),
                selectedItem = stateHolder.selectedChip,
                onSelect = stateHolder::selectChip,
                label = { chip ->
                    when (chip) {
                        FeedChip.FOLLOWING -> strings.mediaDetailActivityFollowing
                        FeedChip.GLOBAL -> strings.mediaDetailActivityGlobal
                        FeedChip.REVIEWS -> strings.feedSectionReviews
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            when (stateHolder.section) {
                FeedSection.ACTIVITY -> {
                    when {
                        stateHolder.isLoading -> PlatformListLoading(label = strings.listsLoadingLabel)
                        stateHolder.error != null -> PlatformListMessage(
                            title = strings.listsErrorLabel,
                            tone = PlatformListMessageTone.Error,
                            actionLabel = strings.mediaDetailLoadMoreAction,
                            onAction = { stateHolder.load(forceRefresh = true) },
                        )
                        stateHolder.activities.isEmpty() -> PlatformListMessage(title = strings.listsEmptyLabel)
                        else -> LazyColumn(
                            state = activityListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = stateHolder.activities,
                                key = { it.id },
                            ) { activity ->
                                FeedActivityCard(
                                    activity = activity,
                                    nowEpochSeconds = nowEpochSeconds,
                                    onMediaClick = onMediaClick,
                                )
                            }
                            if (stateHolder.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                FeedSection.REVIEWS -> {
                    when {
                        stateHolder.isLoadingReviews && stateHolder.reviews.isEmpty() ->
                            PlatformListLoading(label = strings.listsLoadingLabel)
                        stateHolder.reviewsError != null && stateHolder.reviews.isEmpty() ->
                            PlatformListMessage(
                                title = strings.listsErrorLabel,
                                tone = PlatformListMessageTone.Error,
                                actionLabel = strings.mediaDetailLoadMoreAction,
                                onAction = { stateHolder.loadReviews(forceRefresh = true) },
                            )
                        stateHolder.reviews.isEmpty() -> PlatformListMessage(title = strings.listsEmptyLabel)
                        else -> LazyColumn(
                            state = reviewsListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = stateHolder.reviews,
                                key = { it.id },
                            ) { review ->
                                ReviewCard(
                                    reviewerName = review.reviewerName,
                                    reviewerImageUrl = review.reviewerImageUrl,
                                    rating = review.rating,
                                    summary = review.summary,
                                    mediaTitle = "${strings.feedReviewOf} ${review.mediaTitle}",
                                    onClick = { onMediaClick(review.mediaId, true) },
                                )
                            }
                            if (stateHolder.isLoadingMoreReviews) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
