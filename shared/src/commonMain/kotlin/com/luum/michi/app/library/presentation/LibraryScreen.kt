package com.luum.michi.app.library.presentation

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformFeaturePlaceholder

@Composable
fun LibraryScreen() {
    val strings = LanguageProvider.strings

    PlatformFeaturePlaceholder(
        title = strings.libraryTitle,
        description = strings.libraryDescription,
    )
}
