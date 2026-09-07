package com.luum.michi.app.ui.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.language.SpanishStrings
import com.luum.michi.app.core.language.getLanguageStrings

val LocalLanguageStrings = staticCompositionLocalOf<LanguageStrings> { SpanishStrings }

object LanguageProvider {
    val strings: LanguageStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalLanguageStrings.current
}

@Composable
fun ProvideLanguageStrings(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val strings = getLanguageStrings(language)
    CompositionLocalProvider(LocalLanguageStrings provides strings) {
        content()
    }
}
