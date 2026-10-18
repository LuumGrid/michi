package com.luum.michi.app.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.label
import com.luum.michi.app.notifications.ui.model.dispatch
import com.luum.michi.app.notifications.ui.model.toRowItem
import com.luum.michi.app.notifications.ui.state.NotificationsStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaCoverCard
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * The notifications feed, a full page over the tab (BELOW_DETAIL), like the
 * calendar. Opening it is reading it: [NotificationsStateHolder.onOpen] resets
 * the server count (0 extra requests). Tapping a media notification pushes its
 * detail over the page, so back returns to the feed with its position.
 */
@Composable
internal fun NotificationsScreen(
    holder: NotificationsStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(holder) { holder.onOpen() }
    NotificationsContent(
        holder = holder,
        strings = strings,
        bottomPadding = bottomPadding,
        onRetry = onRetry,
        onRefresh = onRefresh,
        onOpenMedia = onOpenMedia,
        onOpenUrl = onOpenUrl,
        modifier = modifier,
    )
}

@Composable
private fun NotificationsContent(
    holder: NotificationsStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    val hasContent = holder.notifications.isNotEmpty()
    val chips = remember(strings) {
        NotificationFilter.entries.map { ChipTab(value = it, label = it.label(strings)) }
    }
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        ChipRail(
            tabs = chips,
            selected = holder.selectedFilter,
            onSelect = { holder.selectFilter(it) },
        )
        LoadStateContainer(
            hasContent = hasContent,
            isLoading = holder.isLoading,
            errorMessage = errorMessage,
            loadingLabel = strings.notificationsLoadingLabel,
            waitingLabel = strings.networkThrottleWaitingLabel,
            errorTitle = strings.notificationsErrorLabel,
            retryLabel = strings.retryAction,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
            refreshError = if (hasContent) errorMessage else null,
            actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holder.clearActionError() },
            empty = {
                MessagePanel(
                    title = strings.notificationsEmptyLabel,
                    message = null,
                    icon = AppIcons.Notifications,
                )
            },
        ) {
            // Changing filter empties the list: the scroll returns to the top.
            val listState = rememberResetListState(holder.selectedFilter)
            LoadMoreEffect(
                state = listState,
                enabled = holder.hasNextPage,
                loadedPage = holder.currentPage,
                onLoadMore = { holder.loadMore() },
            )
            // One "now" for the whole pass; the screen is rebuilt on each open.
            val now = remember { currentEpochSeconds() }
            val items = remember(holder.notifications, strings, now) {
                holder.notifications.map { it.toRowItem(strings, now) }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp + bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = items,
                    key = { item -> item.id },
                ) { item ->
                    MediaCoverCard(
                        cover = item.image,
                        title = item.text,
                        subtitle = item.time,
                        onClick = { item.target.dispatch(onOpenMedia, onOpenUrl) },
                    )
                }
                if (holder.hasNextPage) {
                    item(key = LoadMoreKey) { LoadMoreFooter(isLoading = holder.isLoadingMore) }
                }
            }
        }
    }
}
