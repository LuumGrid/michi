package com.luum.michi.app.discovery.presentation

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformFeaturePlaceholder

@Composable
fun DiscoveryScreen() {
    val strings = LanguageProvider.strings

    PlatformFeaturePlaceholder(
        title = strings.discoveryTitle,
        description = strings.discoveryDescription,
    )
}
