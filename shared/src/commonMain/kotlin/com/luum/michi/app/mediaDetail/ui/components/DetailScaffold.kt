package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.ui.components.LoadStateContainer

/**
 * The load/error/refresh switch shared by the four detail scopes (media,
 * character, staff, studio). Fixes the detail labels so the four screens cannot
 * drift: [LoadStateContainer] with the detail loading/error copy and the common
 * retry/dismiss labels. A load error on top of content already shown becomes the
 * compact refresh row.
 */
@Composable
internal fun DetailLoadState(
    hasContent: Boolean,
    isLoading: Boolean,
    error: NetworkError?,
    actionError: NetworkError?,
    onRetry: () -> Unit,
    onDismissActionError: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LoadStateContainer(
        hasContent = hasContent,
        isLoading = isLoading,
        errorMessage = error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (hasContent) error?.let { strings.networkErrorMessage(it) } else null,
        actionError = actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = onDismissActionError,
        content = content,
    )
}

/**
 * The scrolling column shared by the media/character/staff detail pages:
 * `fillMaxSize`, a 24dp bottom inset plus the host's [bottomPadding] and 20dp
 * between sections. The studio page uses the poster grid instead.
 */
@Composable
internal fun DetailPageColumn(
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}
