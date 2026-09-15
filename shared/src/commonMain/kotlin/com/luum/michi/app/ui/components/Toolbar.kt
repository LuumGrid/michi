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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Trailing action. Consecutive actions sharing a non-null [group] merge
 * into one glass capsule (e.g. filter + sort); a null group always stands
 * alone as its own circle — merging is explicit, never by omission.
 * Search always stays ungrouped: lone circle in its place.
 * The primary action goes last.
 */
internal data class ToolbarAction(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String,
    val badgeCount: Int? = null,
    val group: String? = null,
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
 *   consecutive same-group actions merge into one glass capsule (iOS Liquid
 *   Glass grouping), a lone action is its own glass circle. Reference shape:
 *   [filter + sort] capsule with the search circle standing apart.
 * - search: full-width glass [SearchField] pill with back-to-cancel on the
 *   left and clear-X on the right; system back must also map to [onSearchClose].
 *
 * Collapses on scroll via [scrollBehavior] (wired by `root` only for
 * explicitly opted-in surfaces; null keeps it pinned).
 *
 * Only primitives cross this boundary; results/scrim are drawn by `root`.
 *
 * Set [applyWindowInsets] = false when hosting inside a sheet: the status-bar
 * insets only make sense at the top level.
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
    applyWindowInsets: Boolean = true,
) {
    val insets = if (applyWindowInsets) {
        Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)
    } else {
        Modifier
    }
    AnimatedContent(
        // Full object as state (so open/close keeps the last query), but the
        // transition key is just open/closed: typing updates the text in
        // place instead of restarting the animation (lost focus otherwise).
        targetState = search,
        contentKey = { it != null },
        transitionSpec = {
            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                (fadeOut() + slideOutHorizontally { -it / 4 })
        },
        modifier = modifier,
        label = "toolbar-search",
    ) { activeSearch ->
        if (activeSearch == null) {
            // Own row (not TopAppBar): exact 16dp edges matching the tab capsule.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(insets)
                    .toolbarCollapse(scrollBehavior)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (navigation is ToolbarNavigation.Back) {
                    GlassCircleButton(
                        icon = AppIcons.Back,
                        contentDescription = navigation.label,
                        onClick = onNavigation,
                    )
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
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                }
                if (actions.size == 1) {
                    val action = actions.first()
                    GlassCircleButton(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        onClick = { onAction(action.id) },
                        badgeCount = action.badgeCount,
                    )
                } else if (actions.size > 1) {
                    actionRuns(actions).forEachIndexed { index, run ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        if (run.size == 1) {
                            val action = run.first()
                            GlassCircleButton(
                                icon = action.icon,
                                contentDescription = action.contentDescription,
                                onClick = { onAction(action.id) },
                                badgeCount = action.badgeCount,
                            )
                        } else {
                            Surface(
                                shape = GlassShape,
                                color = glassContainerColor(),
                                border = glassBorder(),
                                modifier = Modifier.glass(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    run.forEach { action ->
                                        ToolbarActionButton(action = action, onAction = onAction)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Three loose glass pieces (never merged): chevron circle mirroring
            // the search action position, expanding text pill, X circle where
            // the magnifier was. Borders may touch, surfaces stay separate.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(insets)
                    .toolbarCollapse(scrollBehavior)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(
                    icon = AppIcons.Back,
                    contentDescription = backContentDescription,
                    onClick = onSearchClose,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    shape = GlassShape,
                    color = glassContainerColor(),
                    border = glassBorder(),
                    modifier = Modifier
                        .weight(1f)
                        .glass(),
                ) {
                    // Lambda param (not the closure): during the close fade the
                    // outgoing side still sees the last query instead of null.
                    SearchField(
                        query = activeSearch.query,
                        hint = activeSearch.hint,
                        autoFocus = activeSearch.autoFocus,
                        onQueryChange = onSearchChange,
                        onSubmit = onSearchSubmit,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                // Always visible, even with an empty query (clear is a no-op then).
                GlassCircleButton(
                    icon = AppIcons.Clear,
                    contentDescription = clearContentDescription.orEmpty(),
                    onClick = { onSearchClear() },
                )
            }
        }
    }
}

/**
 * Manual enter-always collapse shared by both toolbar states: reports its
 * own height as the scroll limit and offsets by the shared behavior.
 * Read inside composition so scroll updates recompose.
 */
/**
 * Consecutive runs sharing the same non-null [ToolbarAction.group]. Null
 * never merges: every ungrouped action is its own run (lone circle).
 * Each run renders as one floating piece: lone circle or merged capsule.
 */
private fun actionRuns(actions: List<ToolbarAction>): List<List<ToolbarAction>> {
    val runs = mutableListOf<MutableList<ToolbarAction>>()
    actions.forEach { action ->
        val current = runs.lastOrNull()
        if (action.group != null && current != null && current.first().group == action.group) {
            current.add(action)
        } else {
            runs.add(mutableListOf(action))
        }
    }
    return runs
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.toolbarCollapse(scrollBehavior: TopAppBarScrollBehavior?): Modifier {
    val offsetPx = scrollBehavior?.state?.heightOffset?.roundToInt() ?: 0
    return this
        .onSizeChanged { size ->
            scrollBehavior?.state?.heightOffsetLimit = -size.height.toFloat()
        }
        .offset { IntOffset(x = 0, y = offsetPx) }
}

@Composable
private fun ToolbarActionButton(
    action: ToolbarAction,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fixed 48dp target, same as GlassCircleButton: uniform everywhere.
    IconButton(
        onClick = { onAction(action.id) },
        modifier = modifier,
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
