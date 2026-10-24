package com.luum.michi.app.settings.domain.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort

internal enum class ThemeMode { SYSTEM, LIGHT, DARK }

internal fun ThemeMode.label(strings: LanguageStrings): String = when (this) {
    ThemeMode.SYSTEM -> strings.settingsThemeSystem
    ThemeMode.LIGHT -> strings.settingsThemeLight
    ThemeMode.DARK -> strings.settingsThemeDark
}

internal enum class TitleLanguage { ROMAJI, ENGLISH, NATIVE }

/** Parses AniList's raw `UserTitleLanguage` value (`Viewer.options.titleLanguage`). */
internal fun parseTitleLanguage(raw: String?): TitleLanguage = when (raw) {
    "ENGLISH" -> TitleLanguage.ENGLISH
    "NATIVE" -> TitleLanguage.NATIVE
    else -> TitleLanguage.ROMAJI
}

internal fun TitleLanguage.label(strings: LanguageStrings): String = when (this) {
    TitleLanguage.ROMAJI -> strings.settingsTitleLanguageRomaji
    TitleLanguage.ENGLISH -> strings.settingsTitleLanguageEnglish
    TitleLanguage.NATIVE -> strings.settingsTitleLanguageNative
}

internal enum class StaffNameLanguage { ROMAJI_WESTERN, ROMAJI, NATIVE }

internal fun StaffNameLanguage.label(strings: LanguageStrings): String = when (this) {
    StaffNameLanguage.ROMAJI_WESTERN -> strings.settingsStaffNameWestern
    StaffNameLanguage.ROMAJI -> strings.settingsStaffNameRomaji
    StaffNameLanguage.NATIVE -> strings.settingsStaffNameNative
}

internal enum class ListSort {
    TITLE,
    SCORE,
    UPDATED,
    ADDED,
}

internal fun ListSort.label(strings: LanguageStrings): String = when (this) {
    ListSort.TITLE -> strings.sortTitle
    ListSort.SCORE -> strings.settingsListSortByScore
    ListSort.UPDATED -> strings.settingsListSortByUpdated
    ListSort.ADDED -> strings.settingsListSortByAdded
}

/**
 * Default sort as a list sort: TITLE orders A-Z like AniList web, the rest
 * newest/highest first. Consumed by Root's hydration chain
 * (saved persist-sort wins, then this, then FOLLOW_LIST).
 */
internal fun ListSort.toUserSort(): Pair<UserListSort, UserListOrder> = when (this) {
    ListSort.TITLE -> UserListSort.TITLE to UserListOrder.ASCENDING
    ListSort.SCORE -> UserListSort.SCORE to UserListOrder.DESCENDING
    ListSort.UPDATED -> UserListSort.LAST_UPDATED to UserListOrder.DESCENDING
    ListSort.ADDED -> UserListSort.LAST_ADDED to UserListOrder.DESCENDING
}

/** Where the shell opens. Local-only ("Pestaña al abrir"). */
internal enum class StartTabOption { DISCOVER, ANIME, MANGA, PROFILE }

internal fun StartTabOption.label(strings: LanguageStrings): String = when (this) {
    StartTabOption.DISCOVER -> strings.discoverLabel
    StartTabOption.ANIME -> strings.animeLabel
    StartTabOption.MANGA -> strings.mangaLabel
    StartTabOption.PROFILE -> strings.profileLabel
}

/** Initial catalog type of the Dashboard (sub-choice when the start tab is DISCOVER). */
internal enum class StartCatalogOption { ANIME, MANGA }

internal fun StartCatalogOption.label(strings: LanguageStrings): String = when (this) {
    StartCatalogOption.ANIME -> strings.animeLabel
    StartCatalogOption.MANGA -> strings.mangaLabel
}

internal data class NotificationPreferences(
    val airing: Boolean = true,
    val activity: Boolean = true,
    val following: Boolean = true,
    val forum: Boolean = false,
    val messages: Boolean = true,
    val media: Boolean = true,
)
