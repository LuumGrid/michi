package com.luum.michi.app.shell.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons

internal enum class ShellBottomTab {
    ANIMATION,
    READING,
    HOME,
    FEED,
    ACCOUNT,
}

internal fun ShellBottomTab.label(strings: LanguageStrings): String = when (this) {
    ShellBottomTab.HOME -> strings.tabHome
    ShellBottomTab.ANIMATION -> strings.tabAnimation
    ShellBottomTab.READING -> strings.tabReading
    ShellBottomTab.FEED -> strings.tabFeed
    ShellBottomTab.ACCOUNT -> strings.tabAccount
}

@Composable
internal fun ShellBottomNavBar(
    selected: ShellBottomTab,
    onSelect: (ShellBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShellBottomTab.entries.forEachIndexed { index, tab ->
                val isFirst = index == 0
                val isLast = index == ShellBottomTab.entries.lastIndex
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
                        )
                        .padding(
                            start = if (isFirst) 4.dp else 0.dp,
                            end = if (isLast) 4.dp else 0.dp,
                            top = 4.dp,
                            bottom = 4.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .scale(pillScale)
                            .background(
                                color = pillColor,
                                shape = CircleShape,
                            ),
                    )
                    Icon(
                        painter = when (tab) {
                            ShellBottomTab.HOME -> PlatformIcons.Home
                            ShellBottomTab.ANIMATION -> PlatformIcons.Animation
                            ShellBottomTab.READING -> PlatformIcons.Reading
                            ShellBottomTab.FEED -> PlatformIcons.Feed
                            ShellBottomTab.ACCOUNT -> PlatformIcons.Account
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-8).dp),
                    )
                    Text(
                        text = tab.label(strings),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                    )
                }
            }
        }
    }
}
