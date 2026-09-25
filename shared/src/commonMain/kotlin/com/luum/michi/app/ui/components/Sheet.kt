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
 * over a scrollable caller-owned interior, plus an optional fixed [footer]
 * (e.g. [SheetActionBar]) pinned below the scroll — existing callers pass
 * nothing and render exactly as before. Dismiss via the X, the native
 * handle, tap-outside or system back. Used by language/theme pickers today;
 * filters and editors tomorrow. Takes only primitives + slots, so no
 * feature model leaks in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalSheet(
    title: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    // Extra header actions ahead of the fixed X (e.g. the language switch
    // on the sign-in sheet). Empty everywhere else — existing callers
    // render exactly as before.
    headerActions: List<ToolbarAction> = emptyList(),
    onHeaderAction: (String) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { SheetDragHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                // 24dp matches the TabBar's bottom margin, so sheet
                // buttons rest at the same air from the system handle.
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Toolbar(
                title = title,
                navigation = ToolbarNavigation.None,
                actions = headerActions + listOf(
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
                onAction = { id ->
                    if (id == ACTION_DISMISS) onDismiss() else onHeaderAction(id)
                },
                onSearchChange = {},
                onSearchSubmit = {},
                onSearchClose = {},
                applyWindowInsets = false,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .weight(1f, fill = false),
            ) {
                content()
            }
            if (footer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp),
                ) {
                    footer()
                }
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
