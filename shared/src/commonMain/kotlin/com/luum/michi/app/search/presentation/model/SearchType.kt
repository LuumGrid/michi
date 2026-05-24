package com.luum.michi.app.search.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class SearchType { ALL, ANIME, MANGA }

internal fun SearchType.label(strings: LanguageStrings): String = when (this) {
    SearchType.ALL -> strings.searchTypeAll
    SearchType.ANIME -> strings.tabAnimation
    SearchType.MANGA -> strings.tabReading
}

internal fun SearchType.toApiValue(): String? = when (this) {
    SearchType.ALL -> null
    SearchType.ANIME -> "ANIME"
    SearchType.MANGA -> "MANGA"
}
