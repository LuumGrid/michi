package com.luum.michi.app.media.presentation

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformFeaturePlaceholder

@Composable
fun MediaScreen() {
    val strings = LanguageProvider.strings

    PlatformFeaturePlaceholder(
        title = strings.mediaDetailTitle,
        description = strings.mediaDetailDescription,
    )
}
