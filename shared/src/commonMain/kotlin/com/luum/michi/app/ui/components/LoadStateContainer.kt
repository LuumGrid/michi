package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Loading / error / content switch shared by the detail screens and the
 * dashboard. Spinner + [loadingLabel] while there is nothing to show yet, a
 * retry [MessagePanel] ([errorTitle]) when the first load failed, and the
 * pull-to-refresh wrapped [content] once [hasContent]. Labels arrive as
 * parameters so this stays feature-agnostic.
 *
 * [refreshError] adds a compact, non-blocking line above the content when a
 * refresh failed while there is already something to show; callers that do not
 * want it (the detail screens) leave it null. [actionError] is the same row for
 * a failed action (a page load, a favourite): it carries a dismiss button
 * instead of a retry, since re-running the action is the user's call.
 */
@Composable
internal fun LoadStateContainer(
    hasContent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    loadingLabel: String,
    errorTitle: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    refreshError: String? = null,
    actionError: String? = null,
    dismissLabel: String? = null,
    onDismissActionError: (() -> Unit)? = null,
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
                        text = loadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        !hasContent && errorMessage != null -> {
            MessagePanel(
                title = errorTitle,
                message = errorMessage,
                icon = Icons.Filled.Warning,
                actionLabel = retryLabel,
                onAction = onRetry,
                modifier = modifier,
            )
        }
        hasContent -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (refreshError != null) {
                    RefreshErrorRow(
                        message = refreshError,
                        retryLabel = retryLabel,
                        onRetry = onRetry,
                    )
                }
                if (actionError != null) {
                    RefreshErrorRow(
                        message = actionError,
                        dismissLabel = dismissLabel,
                        onDismiss = onDismissActionError,
                    )
                }
                PullRefresh(isRefreshing = isLoading, onRefresh = onRetry) {
                    content()
                }
            }
        }
    }
}

/**
 * Compact, non-blocking error row: content stays visible, only a line shows.
 * Either a retry ([retryLabel]/[onRetry], a failed load/refresh) or a dismiss
 * ([dismissLabel]/[onDismiss], a failed action the user can just repeat).
 */
@Composable
internal fun RefreshErrorRow(
    message: String,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null && retryLabel != null) {
            TextButton(onClick = onRetry) {
                Text(text = retryLabel)
            }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = AppIcons.Clear, contentDescription = dismissLabel)
            }
        }
    }
}
