package com.luum.michi.app.core.navigation.domain

import com.luum.michi.app.core.language.domain.LanguageStrings

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
