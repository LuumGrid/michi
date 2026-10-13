package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh

/**
 * Loading / error / content switch shared by the four detail screens.
 * Spinner + loading label while there is nothing to show yet, a retry
 * [MessagePanel] when the first load failed, and the pull-to-refresh
 * wrapped [content] once [hasContent]. Takes primitives so each holder
 * keeps its own state shape; [onRetry] serves both the retry action and
 * the manual pull-to-refresh.
 */
@Composable
internal fun MediaDetailStateContainer(
    hasContent: Boolean,
    isLoading: Boolean,
    error: NetworkError?,
    strings: LanguageStrings,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when {
        !hasContent && isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.mediaDetailLoadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        !hasContent && error != null -> {
            MessagePanel(
                title = strings.mediaDetailErrorLabel,
                message = strings.networkErrorMessage(error),
                icon = Icons.Filled.Warning,
                actionLabel = strings.retryAction,
                onAction = onRetry,
                modifier = modifier,
            )
        }
        hasContent -> {
            PullRefresh(isRefreshing = isLoading, onRefresh = onRetry, modifier = modifier) {
                content()
            }
        }
    }
}
