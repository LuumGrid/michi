package com.luum.michi.app.settings.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class ThemeMode { SYSTEM, LIGHT, DARK }

internal fun ThemeMode.label(strings: LanguageStrings): String = when (this) {
    ThemeMode.SYSTEM -> strings.settingsThemeSystem
    ThemeMode.LIGHT -> strings.settingsThemeLight
    ThemeMode.DARK -> strings.settingsThemeDark
}

internal enum class TitleLanguage { ROMAJI, ENGLISH, NATIVE }

internal fun TitleLanguage.label(strings: LanguageStrings): String = when (this) {
    TitleLanguage.ROMAJI -> strings.settingsTitleLanguageRomaji
    TitleLanguage.ENGLISH -> strings.settingsTitleLanguageEnglish
    TitleLanguage.NATIVE -> strings.settingsTitleLanguageNative
}

internal enum class ScoreFormat {
    POINT_100,
    POINT_10_DECIMAL,
    POINT_10,
    POINT_5_STARS,
    POINT_3_SMILEYS,
}

internal fun ScoreFormat.label(strings: LanguageStrings): String = when (this) {
    ScoreFormat.POINT_100 -> strings.settingsScoreFormatPoint100
    ScoreFormat.POINT_10_DECIMAL -> strings.settingsScoreFormatPoint10Decimal
    ScoreFormat.POINT_10 -> strings.settingsScoreFormatPoint10
    ScoreFormat.POINT_5_STARS -> strings.settingsScoreFormatPoint5Stars
    ScoreFormat.POINT_3_SMILEYS -> strings.settingsScoreFormatPoint3Smileys
}

internal enum class ListSort {
    TITLE,
    SCORE,
    UPDATED,
    ADDED,
    RELEASE,
}

internal fun ListSort.label(strings: LanguageStrings): String = when (this) {
    ListSort.TITLE -> strings.settingsListSortByTitle
    ListSort.SCORE -> strings.settingsListSortByScore
    ListSort.UPDATED -> strings.settingsListSortByUpdated
    ListSort.ADDED -> strings.settingsListSortByAdded
    ListSort.RELEASE -> strings.settingsListSortByRelease
}

internal enum class HomeTabOption { HOME, ANIMATION, READING, ACCOUNT }

internal fun HomeTabOption.label(strings: LanguageStrings): String = when (this) {
    HomeTabOption.HOME -> strings.tabHome
    HomeTabOption.ANIMATION -> strings.tabAnimation
    HomeTabOption.READING -> strings.tabReading
    HomeTabOption.ACCOUNT -> strings.tabAccount
}

internal data class NotificationPreferences(
    val airing: Boolean = true,
    val activity: Boolean = true,
    val following: Boolean = true,
    val forum: Boolean = false,
    val messages: Boolean = true,
    val media: Boolean = true,
)
