package com.luum.michi.app.profile.ui.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.profile.ui.state.ProfileReviewsStateHolder
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
 * A paginated list of the viewer's reviews (newest first): the reviewed media's
 * cover and title, the review's summary (two lines) and its 0-100 score. Tapping
 * a review opens the reviewed media's detail. The list title lives in the toolbar.
 */
@Composable
internal fun ProfileReviewsScreen(
    holder: ProfileReviewsStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = holder.reviews.isNotEmpty()
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    LoadStateContainer(
        hasContent = hasContent,
        isLoading = holder.isLoading,
        errorMessage = errorMessage,
        loadingLabel = strings.profileReviewsLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.profileReviewsErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (hasContent) errorMessage else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
        empty = {
            MessagePanel(
                title = strings.profileReviewsEmptyLabel,
                message = null,
                icon = AppIcons.StarOutline,
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
            items(items = holder.reviews, key = { it.id }) { review ->
                MediaCoverCard(
                    cover = review.mediaCover,
                    title = review.mediaTitle,
                    subtitle = review.summary.ifBlank { null },
                    subtitleMaxLines = 2,
                    titleTrailing = {
                        Text(
                            text = strings.profileReviewScoreLabel(review.score),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { onOpenMedia(review.mediaId) },
                )
            }
            if (holder.hasNextPage) {
                item(key = LoadMoreKey) { LoadMoreFooter(isLoading = holder.isLoadingMore) }
            }
        }
    }
}
