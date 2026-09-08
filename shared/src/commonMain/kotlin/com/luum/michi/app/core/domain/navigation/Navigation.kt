package com.luum.michi.app.core.domain.navigation

import com.luum.michi.app.core.domain.language.LanguageStrings

internal enum class TabSection {
    DISCOVER,
    ANIME,
    MANGA,
    ACCOUNT,
}

internal fun TabSection.label(strings: LanguageStrings): String = when (this) {
    TabSection.DISCOVER -> strings.discoverLabel
    TabSection.ANIME -> strings.animeLabel
    TabSection.MANGA -> strings.mangaLabel
    TabSection.ACCOUNT -> strings.accountLabel
}
