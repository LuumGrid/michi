package com.luum.michi.app.core.domain.navigation

import com.luum.michi.app.core.domain.language.LanguageStrings

internal enum class TabSection {
    DISCOVER,
    ANIME,
    MANGA,
    ACCOUNT,
}

internal fun TabSection.label(strings: LanguageStrings): String = when (this) {
    TabSection.DISCOVER -> strings.tabDiscover
    TabSection.ANIME -> strings.tabAnime
    TabSection.MANGA -> strings.tabManga
    TabSection.ACCOUNT -> strings.tabAccount
}
