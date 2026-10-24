package com.luum.michi.app.profile.ui.follows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.profile.ui.model.aniListUrl
import com.luum.michi.app.profile.ui.state.ProfileFollowsStateHolder
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
 * A paginated list of users (following or followers): the shared
 * [MediaCoverCard] with the user's avatar as its cover. Tapping a user opens
 * their AniList profile on the web (no in-app user page exists). The list title
 * lives in the toolbar.
 */
@Composable
internal fun ProfileFollowsScreen(
    holder: ProfileFollowsStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = holder.follows.isNotEmpty()
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    LoadStateContainer(
        hasContent = hasContent,
        isLoading = holder.isLoading,
        errorMessage = errorMessage,
        loadingLabel = strings.profileFollowsLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.profileFollowsErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (hasContent) errorMessage else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
        empty = {
            MessagePanel(
                title = strings.profileFollowsEmptyLabel,
                message = null,
                icon = AppIcons.Groups,
            )
        },
    ) {
        val listState = rememberResetListState(holder)
        LoadMoreEffect(
            state = listState,
            enabled = holder.hasNextPage,
            loadedPage = holder.currentPage,
            onLoadMore = { holder.loadMore() },
        )
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
            items(items = holder.follows, key = { it.id }) { follow ->
                MediaCoverCard(
                    cover = follow.avatar,
                    title = follow.name,
                    onClick = { onOpenUser(follow.aniListUrl()) },
                )
            }
            if (holder.hasNextPage) {
                item(key = LoadMoreKey) { LoadMoreFooter(isLoading = holder.isLoadingMore) }
            }
        }
    }
}
