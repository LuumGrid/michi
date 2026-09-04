package com.luum.michi.app.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformBottomBarCircle

/**
 * Cluster inferior global: [tab bar + círculo de search pegado, sin separación],
 * para que la tab bar conserve todo el ancho y sus 4 tabs respiren.
 */
@Composable
internal fun ShellBottomCluster(
    selectedTab: ShellTabSection,
    onSearchClick: () -> Unit,
    onSelectTab: (ShellTabSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ShellTabBar(
                selected = selectedTab,
                onSelect = onSelectTab,
            )
        }
        ShellSearchCircle(onClick = onSearchClick)
    }
}

/** Círculo de search aislado con el estilo de la tab bar. */
@Composable
internal fun ShellSearchCircle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LanguageProvider.strings
    PlatformBottomBarCircle(onClick = onClick, modifier = modifier) {
        Icon(
            painter = PlatformIcons.Search,
            contentDescription = strings.searchTitle,
            modifier = Modifier.size(24.dp),
        )
    }
}
