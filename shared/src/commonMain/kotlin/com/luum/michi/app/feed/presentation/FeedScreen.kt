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
import com.luum.michi.app.feed.data.FeedFilter
import com.luum.michi.app.feed.presentation.components.FeedActivityCard
import com.luum.michi.app.feed.presentation.state.FeedStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreen(
    stateHolder: FeedStateHolder,
    onMediaClick: (mediaId: Int, isAnime: Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings
    val nowEpochSeconds = remember { currentEpochSeconds() }
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(nearEnd) {
        if (nearEnd) stateHolder.loadMore()
    }

    LaunchedEffect(Unit) {
        stateHolder.load()
    }

    LaunchedEffect(stateHolder.filter) {
        listState.scrollToItem(0)
    }

    PullToRefreshBox(
        isRefreshing = stateHolder.isRefreshing,
        onRefresh = { stateHolder.load(forceRefresh = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlatformChips(
            items = FeedFilter.entries,
            selectedItem = stateHolder.filter,
            onSelect = stateHolder::selectFilter,
            label = { filter ->
                when (filter) {
                    FeedFilter.FOLLOWING -> strings.mediaDetailActivityFollowing
                    FeedFilter.GLOBAL -> strings.mediaDetailActivityGlobal
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

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
                state = listState,
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
    }
}
