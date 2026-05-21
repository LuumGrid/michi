package com.luum.michi.app.search.presentation

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformFeaturePlaceholder

@Composable
fun SearchScreen() {
    val strings = LanguageProvider.strings

    PlatformFeaturePlaceholder(
        title = strings.searchTitle,
        description = strings.searchDescription,
    )
}
