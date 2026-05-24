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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    HOME,
    ANIMATION,
    READING,
    ACCOUNT,
}

internal fun ShellBottomTab.label(strings: LanguageStrings): String = when (this) {
    ShellBottomTab.HOME -> strings.tabHome
    ShellBottomTab.ANIMATION -> strings.tabAnimation
    ShellBottomTab.READING -> strings.tabReading
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
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 32.dp)
                            .scale(pillScale)
                            .background(
                                color = pillColor,
                                shape = RoundedCornerShape(16.dp),
                            ),
                    )
                    Icon(
                        painter = when (tab) {
                            ShellBottomTab.HOME -> PlatformIcons.Home
                            ShellBottomTab.ANIMATION -> PlatformIcons.Animation
                            ShellBottomTab.READING -> PlatformIcons.Reading
                            ShellBottomTab.ACCOUNT -> PlatformIcons.Account
                        },
                        contentDescription = tab.label(strings),
                        tint = iconColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
