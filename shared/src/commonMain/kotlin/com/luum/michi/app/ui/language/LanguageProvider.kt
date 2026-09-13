package com.luum.michi.app.ui.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.TabSection

/**
 * Exposes the active [LanguageStrings], mirroring the theme provider.
 * Tables stay in `core/language/domain`; this is only the CompositionLocal.
 */
internal val LocalStrings = compositionLocalOf<LanguageStrings> { EnglishStrings }

internal object Strings {
    val current: LanguageStrings
        @Composable get() = LocalStrings.current
}

/**
 * In-context search hint per tab: DISCOVER searches the global catalog,
 * ANIME/MANGA search the user's own list. ACCOUNT has no search.
 */
internal fun searchHintFor(tab: TabSection, strings: LanguageStrings): String? = when (tab) {
    TabSection.DISCOVER -> strings.discoverSearchPlaceholder
    TabSection.ANIME, TabSection.MANGA -> strings.searchEmptyQueryHint
    TabSection.ACCOUNT -> null
}
