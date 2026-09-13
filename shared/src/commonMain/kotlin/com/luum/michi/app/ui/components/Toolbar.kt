package com.luum.michi.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Toolbar navigation slot. Leading edge only: standard back affordance
 * (Apple HIG: use the standard Back button, one primary action trailing).
 */
internal sealed interface ToolbarNavigation {
    data object None : ToolbarNavigation
    data class Back(val label: String) : ToolbarNavigation
}

/** Trailing action. Keep to max 2 (+search); the primary action goes last. */
internal data class ToolbarAction(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String,
    val badgeCount: Int? = null,
)

/**
 * In-context search state. Non-null means the toolbar is transformed:
 * title + actions are replaced by a full-width [SearchField].
 * The [hint] declares the scope ("Buscar en mi lista..." vs global catalog).
 */
internal data class ToolbarSearch(
    val query: String,
    val hint: String,
    val autoFocus: Boolean = true,
)

/**
 * App toolbar with two states (Apple HIG toolbar pattern, in-context search
 * like WhatsApp/Gmail/Play Store):
 * - normal: plain title (no pill, like Netflix) + floating glass actions —
 *   a lone action is its own glass circle, same-side actions merge into one
 *   glass capsule (iOS Liquid Glass grouping).
 * - search: full-width glass [SearchField] pill with back-to-cancel on the
 *   left and clear-X on the right; system back must also map to [onSearchClose].
 *
 * Collapses on scroll via [scrollBehavior] (wired by `root` with enter-always).
 *
 * Only primitives cross this boundary; results/scrim are drawn by `root`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Toolbar(
    title: String,
    navigation: ToolbarNavigation,
    actions: List<ToolbarAction>,
    search: ToolbarSearch?,
    backContentDescription: String?,
    clearContentDescription: String?,
    onNavigation: () -> Unit,
    onAction: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchClear: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
    AnimatedContent(
        targetState = search,
        transitionSpec = {
            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                (fadeOut() + slideOutHorizontally { -it / 4 })
        },
        modifier = modifier,
        label = "toolbar-search",
    ) { activeSearch ->
        if (activeSearch == null) {
            // Own row (not TopAppBar): exact 16dp edges matching the tab capsule.
            // Collapse is driven manually from the shared scroll behavior.
            val offsetPx = scrollBehavior?.state?.heightOffset?.roundToInt() ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .onSizeChanged { size ->
                        scrollBehavior?.state?.heightOffsetLimit = -size.height.toFloat()
                    }
                    .offset { IntOffset(x = 0, y = offsetPx) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (navigation is ToolbarNavigation.Back) {
                    Surface(
                        shape = GlassCircle,
                        color = glassContainerColor(),
                        border = glassBorder(),
                        modifier = Modifier.glass(GlassCircle),
                    ) {
                        IconButton(onClick = onNavigation) {
                            Icon(
                                imageVector = AppIcons.Back,
                                contentDescription = navigation.label,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }
                // Same fade as bubble + content (see TabMotion.kt): one rhythm.
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        (fadeIn(animationSpec = tabFadeSpec()) togetherWith
                            fadeOut(animationSpec = tabFadeSpec())) using
                            SizeTransform(clip = false)
                    },
                    label = "toolbar-title",
                    modifier = Modifier.weight(1f),
                ) { activeTitle ->
                    Text(
                        text = activeTitle,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                }
                if (actions.size == 1) {
                    val action = actions.first()
                    Surface(
                        shape = GlassCircle,
                        color = glassContainerColor(),
                        border = glassBorder(),
                        modifier = Modifier.glass(GlassCircle),
                    ) {
                        ToolbarActionButton(action = action, onAction = onAction)
                    }
                } else if (actions.size > 1) {
                    Surface(
                        shape = GlassShape,
                        color = glassContainerColor(),
                        border = glassBorder(),
                        modifier = Modifier.glass(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            actions.forEach { action ->
                                ToolbarActionButton(action = action, onAction = onAction)
                            }
                        }
                    }
                }
            }
        } else {
    Surface(
        modifier = Modifier
            .glassPadding(top = true)
            .glass(),
        shape = GlassShape,
        color = glassContainerColor(),
        border = glassBorder(),
    ) {
            TopAppBar(
                title = {
                    SearchField(
                        query = activeSearch.query,
                        hint = activeSearch.hint,
                        autoFocus = activeSearch.autoFocus,
                        backContentDescription = backContentDescription,
                        clearContentDescription = clearContentDescription,
                        onQueryChange = onSearchChange,
                        onSubmit = onSearchSubmit,
                        onClear = onSearchClear,
                        onBack = onSearchClose,
                    )
                },
                navigationIcon = {},
                colors = barColors,
                scrollBehavior = scrollBehavior,
            )
    }
        }
    }
}

@Composable
private fun ToolbarActionButton(
    action: ToolbarAction,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onAction(action.id) },
        modifier = modifier.size(40.dp),
    ) {
        if (action.badgeCount != null && action.badgeCount > 0) {
            BadgedBox(badge = { Text(text = action.badgeCount.toString()) }) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                )
            }
        } else {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
            )
        }
    }
}
