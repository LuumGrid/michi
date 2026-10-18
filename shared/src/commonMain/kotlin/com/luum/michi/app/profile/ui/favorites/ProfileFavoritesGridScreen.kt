package com.luum.michi.app.profile.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.ui.components.ProfileFavoriteRemoveButton
import com.luum.michi.app.profile.ui.model.toRailItem
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.ui.components.DetailPosterGrid
import com.luum.michi.app.ui.components.LoadMoreEffect
import com.luum.michi.app.ui.components.LoadMoreFooter
import com.luum.michi.app.ui.components.LoadMoreKey
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * The paginated "View all" of one favourites category, and where favourites are
 * removed. Media and people render as the shared poster grid; studios (no image)
 * render as settings-style rows. In edit mode every item shows a remove button
 * and tapping it does not navigate. The category title lives in the toolbar.
 */
@Composable
internal fun ProfileFavoritesGridScreen(
    holder: ProfileFavoritesGridStateHolder,
    category: ProfileFavoritesCategory,
    isEditing: Boolean,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpen: (Int) -> Unit,
    onRemove: (Int) -> Unit,
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
        empty = {
            MessagePanel(
                title = strings.profileFavoritesGridEmptyLabel,
                message = null,
                icon = AppIcons.Favorite,
            )
        },
    ) {
        if (category == ProfileFavoritesCategory.STUDIOS) {
            StudioFavoriteRows(
                holder = holder,
                isEditing = isEditing,
                removeDescription = strings.profileRemoveFavoriteAction,
                bottomPadding = bottomPadding,
                onSelect = onOpen,
                onRemove = onRemove,
            )
        } else {
            val items = remember(holder.items) { holder.items.map { it.toRailItem() } }
            DetailPosterGrid(
                items = items,
                onSelect = onOpen,
                isLoading = holder.isLoading,
                isLoadingMore = holder.isLoadingMore,
                hasNextPage = holder.hasNextPage,
                // A finished load (not the page number) re-arms the footer, so a
                // post-removal re-read of the same page still advances.
                loadedPage = holder.loadKey,
                onLoadMore = { holder.loadMore() },
                emptyLabel = strings.profileFavoritesGridEmptyLabel,
                resetKey = category,
                bottomPadding = bottomPadding,
                cardOverlay = if (isEditing) {
                    { favorite ->
                        ProfileFavoriteRemoveButton(
                            contentDescription = strings.profileRemoveFavoriteAction,
                            onClick = { onRemove(favorite.id) },
                            enabled = !holder.isRemoving(favorite.id),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/** Studio favourites: settings-style rows (name only) in one continuous group,
 *  with the auto-paging footer as its own item. In edit mode the chevron becomes
 *  a remove button and the row does not navigate. */
@Composable
private fun StudioFavoriteRows(
    holder: ProfileFavoritesGridStateHolder,
    isEditing: Boolean,
    removeDescription: String,
    bottomPadding: Dp,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val listState = rememberResetListState(ProfileFavoritesCategory.STUDIOS)
    LoadMoreEffect(
        state = listState,
        enabled = holder.hasNextPage,
        loadedPage = holder.loadKey,
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
                        onClick = if (isEditing) null else ({ onSelect(studio.id) }),
                        divider = index > 0,
                        trailing = if (isEditing) {
                            {
                                ProfileFavoriteRemoveButton(
                                    contentDescription = removeDescription,
                                    onClick = { onRemove(studio.id) },
                                    enabled = !holder.isRemoving(studio.id),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (holder.hasNextPage) {
            item(key = LoadMoreKey) { LoadMoreFooter(isLoading = holder.isLoadingMore) }
        }
    }
}
