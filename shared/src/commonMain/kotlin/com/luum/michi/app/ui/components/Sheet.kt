package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Generic bottom sheet: [Toolbar] header (title + X, no status-bar insets)
 * over a scrollable caller-owned interior. Dismiss via the X, the native
 * handle, tap-outside or system back. Used by language/theme pickers today;
 * filters and editors tomorrow. Takes only primitives + a slot, so no
 * feature model leaks in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalSheet(
    title: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { SheetDragHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Toolbar(
                title = title,
                navigation = ToolbarNavigation.None,
                actions = listOf(
                    ToolbarAction(
                        id = ACTION_DISMISS,
                        icon = AppIcons.Clear,
                        contentDescription = dismissLabel,
                    ),
                ),
                search = null,
                backContentDescription = null,
                clearContentDescription = null,
                onNavigation = {},
                onAction = { onDismiss() },
                onSearchChange = {},
                onSearchSubmit = {},
                onSearchClose = {},
                applyWindowInsets = false,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                content()
            }
        }
    }
}

private const val ACTION_DISMISS = "dismiss"

/**
 * Compact drag handle (standard 32x4 capsule look): tighter than the M3
 * default, whose 16dp vertical padding left dead space above the sheet
 * toolbar. Tune [SHEET_HANDLE_TOP]/[SHEET_HANDLE_BOTTOM] from screenshots.
 */
private val SHEET_HANDLE_TOP = 8.dp
private val SHEET_HANDLE_BOTTOM = 4.dp

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SHEET_HANDLE_TOP, bottom = SHEET_HANDLE_BOTTOM),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(width = 32.dp, height = 4.dp),
        ) {}
    }
}
