package com.luum.michi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.navigation.domain.TabSection

/**
 * Bottom tab bar: 4 top-level destinations (Apple HIG tab bar + UITabBar:
 * navigation only, never actions; always visible except under modal/detail;
 * single-word labels; state preserved per tab by `root`).
 *
 * Floating glass capsule spanning the viewport width (generous touch
 * targets), 24dp bottom margin; content scrolls behind it. Compact custom
 * items (~64dp): the active bubble wraps icon + label together.
 */
internal data class TabItem(
    val section: TabSection,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val badgeCount: Int? = null,
)

@Composable
internal fun TabBar(
    selected: TabSection,
    tabs: List<TabItem>,
    onSelect: (TabSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = GlassCircle,
            color = glassContainerColor(),
            border = glassBorder(),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .glass(GlassCircle),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    TabBarItem(
                        tab = tab,
                        selected = tab.section == selected,
                        onClick = { onSelect(tab.section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabBarItem(
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubble by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        } else {
            Color.Transparent
        },
        animationSpec = tabBubbleSpec(),
        label = "tab-bubble",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tabBubbleSpec(),
        label = "tab-content",
    )
    // Bubble only: color crossfade, no scale — growing on select looks cheap.
    Surface(
        shape = GlassCircle,
        color = bubble,
        modifier = modifier
            .clip(GlassCircle)
            .clickable(role = Role.Tab, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val icon = if (selected) tab.selectedIcon ?: tab.icon else tab.icon
            if (tab.badgeCount != null && tab.badgeCount > 0) {
                BadgedBox(badge = { Badge { Text(text = tab.badgeCount.toString()) } }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(26.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = tab.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}
