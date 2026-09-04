package com.luum.michi.app.shell.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformBottomBarDefaults

internal enum class ShellTabSection {
    DISCOVER,
    ANIME,
    MANGA,
    ACCOUNT,
}

internal fun ShellTabSection.label(strings: LanguageStrings): String = when (this) {
    ShellTabSection.DISCOVER -> strings.tabDiscover
    ShellTabSection.ANIME -> strings.tabAnime
    ShellTabSection.MANGA -> strings.tabManga
    ShellTabSection.ACCOUNT -> strings.tabAccount
}

@Composable
internal fun ShellTabBar(
    selected: ShellTabSection,
    onSelect: (ShellTabSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    val density = LocalDensity.current
    // Measured bounds (x, width) of each tab cell, relative to the Row content.
    val tabBounds = remember { mutableStateMapOf<ShellTabSection, Pair<Dp, Dp>>() }
    val selectedBounds = tabBounds[selected]
    // Single sliding capsule: travels between tabs with a soft spring.
    val capsuleX by animateDpAsState(
        targetValue = selectedBounds?.first ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "navCapsuleX",
    )
    val capsuleWidth by animateDpAsState(
        targetValue = selectedBounds?.second ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "navCapsuleWidth",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(PlatformBottomBarDefaults.PillHeight),
        shape = PlatformBottomBarDefaults.Shape,
        color = PlatformBottomBarDefaults.ContainerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedBounds != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = capsuleX)
                            .width(capsuleWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShellTabSection.entries.forEach { tab ->
                    val isSelected = selected == tab
                    val interactionSource = remember { MutableInteractionSource() }
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            PlatformBottomBarDefaults.ContentColor
                        } else {
                            PlatformBottomBarDefaults.HintColor
                        },
                        animationSpec = tween(250),
                        label = "navIconColor",
                    )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            with(density) {
                                tabBounds[tab] = coordinates.positionInParent().x.toDp() to
                                    coordinates.size.width.toDp()
                            }
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(tab) },
                        )
                        .padding(
                            top = 2.dp,
                            bottom = 2.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = when (tab) {
                            ShellTabSection.DISCOVER -> PlatformIcons.Discover
                            ShellTabSection.ANIME -> PlatformIcons.Anime
                            ShellTabSection.MANGA -> PlatformIcons.Manga
                            ShellTabSection.ACCOUNT -> PlatformIcons.Account
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(28.dp)
                            .offset(y = (-7).dp),
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
}
