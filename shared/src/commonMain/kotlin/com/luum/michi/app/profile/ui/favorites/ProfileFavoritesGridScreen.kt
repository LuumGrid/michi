package com.luum.michi.app.profile.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.ui.model.toRailItem
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.language.Strings

/**
 * The paginated "View all" of one favourites category. Media and people render
 * as the shared poster grid; studios (no image) render as settings-style rows,
 * the same look as Explore's studio results. The category title lives in the
 * toolbar (the bar carries it, like a "View all" list).
 */
@Composable
internal fun ProfileFavoritesGridScreen(
    holder: ProfileFavoritesGridStateHolder,
    category: ProfileFavoritesCategory,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = holder.items.isNotEmpty()
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    LoadStateContainer(
        hasContent = hasContent,
        isLoading = holder.isLoading,
        errorMessage = errorMessage,
        loadingLabel = strings.profileLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.profileErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (hasContent) errorMessage else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
    ) {
        if (category == ProfileFavoritesCategory.STUDIOS) {
            StudioFavoriteRows(
                holder = holder,
                bottomPadding = bottomPadding,
                onSelect = onOpen,
            )
        } else {
            val items = remember(holder.items) { holder.items.map { it.toRailItem() } }
            DetailPosterGrid(
                items = items,
                onSelect = onOpen,
                isLoading = holder.isLoading,
                isLoadingMore = holder.isLoadingMore,
                hasNextPage = holder.hasNextPage,
                loadedPage = holder.currentPage,
                onLoadMore = { holder.loadMore() },
                emptyLabel = strings.profileFavoritesGridEmptyLabel,
                resetKey = category,
                bottomPadding = bottomPadding,
            )
        }
    }
}

/** Studio favourites: settings-style rows (name only) in one continuous group,
 *  with the auto-paging footer as its own item. */
@Composable
private fun StudioFavoriteRows(
    holder: ProfileFavoritesGridStateHolder,
    bottomPadding: Dp,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberResetListState(ProfileFavoritesCategory.STUDIOS)
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
    ) {
        item(key = "studio-rows") {
            OptionGroup(title = null) {
                holder.items.forEachIndexed { index, studio ->
                    SettingsRow(
                        label = studio.name,
                        value = "",
                        onClick = { onSelect(studio.id) },
                        divider = index > 0,
                    )
                }
            }
        }
        if (holder.hasNextPage) {
            item(key = LoadMoreKey) { LoadMoreFooter(isLoading = holder.isLoadingMore) }
        }
    }
}
