package com.luum.michi.app.shell.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.platform.MichiBrand
import com.luum.michi.app.core.platform.PlatformIcons

internal enum class ShellBottomTab(
    val brand: MichiBrand? = null,
) {
    DISCOVERY,
    SEARCH,
    ANIMATION(MichiBrand.ANIMATION),
    ILLUSTRATION(MichiBrand.ILLUSTRATION),
    ACCOUNT,
}

internal fun ShellBottomTab.label(): String = when (this) {
    ShellBottomTab.DISCOVERY -> "Descubrimiento"
    ShellBottomTab.SEARCH -> "Buscar"
    ShellBottomTab.ANIMATION,
    ShellBottomTab.ILLUSTRATION -> requireNotNull(brand).appName
    ShellBottomTab.ACCOUNT -> "Cuenta"
}

@Composable
internal fun ShellBottomNavBar(
    selected: ShellBottomTab,
    onSelect: (ShellBottomTab) -> Unit,
) {
    Column {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        NavigationBar(
            modifier = Modifier.height(65.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets(0),
        ) {
            ShellBottomTab.entries.forEach { tab ->
                val isSelected = selected == tab
                val interactionSource = remember { MutableInteractionSource() }
                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(250),
                    label = "navPillColor",
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.85f,
                    animationSpec = tween(durationMillis = 250),
                    label = "navPillScale",
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(250),
                    label = "navIconColor",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(tab) },
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(width = 64.dp, height = 32.dp)
                            .scale(pillScale)
                            .background(
                                color = pillColor,
                                shape = RoundedCornerShape(16.dp),
                            ),
                    )
                    Icon(
                        painter = when (tab) {
                            ShellBottomTab.DISCOVERY -> PlatformIcons.Discovery
                            ShellBottomTab.SEARCH -> PlatformIcons.Search
                            ShellBottomTab.ANIMATION -> PlatformIcons.Animation
                            ShellBottomTab.ILLUSTRATION -> PlatformIcons.Illustration
                            ShellBottomTab.ACCOUNT -> PlatformIcons.Account
                        },
                        contentDescription = tab.label(),
                        tint = iconColor,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(28.dp),
                    )
                }
            }
        }
    }
}
