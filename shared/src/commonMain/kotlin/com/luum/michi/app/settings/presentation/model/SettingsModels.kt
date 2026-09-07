package com.luum.michi.app.settings.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.ui.Icons

internal data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

internal enum class SettingsItemType {
    DEFAULT,
    THEME,
    LANGUAGE,
    HOME_TAB,
    TITLE_LANGUAGE,
    ADULT_CONTENT,
    SCORE_FORMAT,
    LIST_SORT,
    SPLIT_COMPLETED_ANIME,
    SPLIT_COMPLETED_MANGA,
    ADVANCED_SCORING,
    NOTIFICATIONS,
    MANAGE_ACCOUNT,
    LOGOUT,
    HELP,
    ABOUT,
}

internal val SettingsItemType.isInlineToggle: Boolean
    get() = when (this) {
        SettingsItemType.ADULT_CONTENT,
        SettingsItemType.SPLIT_COMPLETED_ANIME,
        SettingsItemType.SPLIT_COMPLETED_MANGA,
        SettingsItemType.ADVANCED_SCORING -> true
        else -> false
    }

internal val SettingsItemType.isAction: Boolean
    get() = when (this) {
        SettingsItemType.LOGOUT,
        SettingsItemType.MANAGE_ACCOUNT,
        SettingsItemType.HELP -> true
        else -> false
    }

internal data class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: @Composable () -> Painter,
    val type: SettingsItemType = SettingsItemType.DEFAULT,
)

internal fun settingsGroups(strings: LanguageStrings): List<SettingsGroup> = listOf(
    SettingsGroup(
        title = strings.settingsAppSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsThemeTitle,
                subtitle = strings.settingsThemeSubtitle,
                icon = { Icons.Accessibility },
                type = SettingsItemType.THEME,
            ),
            SettingsItem(
                title = strings.settingsLanguageTitle,
                subtitle = strings.settingsLanguageSubtitle,
                icon = { Icons.Language },
                type = SettingsItemType.LANGUAGE,
            ),
            SettingsItem(
                title = strings.settingsDiscoverTabTitle,
                subtitle = strings.settingsDiscoverTabSubtitle,
                icon = { Icons.Discover },
                type = SettingsItemType.HOME_TAB,
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsAniListSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsTitleLanguageTitle,
                subtitle = strings.settingsTitleLanguageSubtitle,
                icon = { Icons.Language },
                type = SettingsItemType.TITLE_LANGUAGE,
            ),
            SettingsItem(
                title = strings.settingsAdultContentTitle,
                subtitle = strings.settingsAdultContentSubtitle,
                icon = { Icons.Privacy },
                type = SettingsItemType.ADULT_CONTENT,
            ),
            SettingsItem(
                title = strings.settingsScoreFormatTitle,
                subtitle = strings.settingsScoreFormatSubtitle,
                icon = { Icons.Like },
                type = SettingsItemType.SCORE_FORMAT,
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsListsSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsListSortTitle,
                subtitle = strings.settingsListSortSubtitle,
                icon = { Icons.Sort },
                type = SettingsItemType.LIST_SORT,
            ),
            SettingsItem(
                title = strings.settingsSplitCompletedAnimeTitle,
                subtitle = strings.settingsSplitCompletedAnimeSubtitle,
                icon = { Icons.Anime },
                type = SettingsItemType.SPLIT_COMPLETED_ANIME,
            ),
            SettingsItem(
                title = strings.settingsSplitCompletedMangaTitle,
                subtitle = strings.settingsSplitCompletedMangaSubtitle,
                icon = { Icons.Manga },
                type = SettingsItemType.SPLIT_COMPLETED_MANGA,
            ),
            SettingsItem(
                title = strings.settingsAdvancedScoringTitle,
                subtitle = strings.settingsAdvancedScoringSubtitle,
                icon = { Icons.ContentPreferences },
                type = SettingsItemType.ADVANCED_SCORING,
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsNotificationsSection,
        items = listOf(
            SettingsItem(
                title = strings.notificationsAction,
                subtitle = strings.settingsNotificationsSubtitle,
                icon = { Icons.Notifications },
                type = SettingsItemType.NOTIFICATIONS,
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsAccountSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsManageAccountTitle,
                subtitle = strings.settingsManageAccountSubtitle,
                icon = { Icons.ManageAccount },
                type = SettingsItemType.MANAGE_ACCOUNT,
            ),
            SettingsItem(
                title = strings.logoutAction,
                icon = { Icons.Logout },
                type = SettingsItemType.LOGOUT,
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsAboutSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsHelpTitle,
                subtitle = strings.settingsHelpSubtitle,
                icon = { Icons.HelpSupport },
                type = SettingsItemType.HELP,
            ),
            SettingsItem(
                title = strings.settingsAboutTitle,
                subtitle = strings.settingsAboutSubtitle,
                icon = { Icons.Information },
                type = SettingsItemType.ABOUT,
            ),
        ),
    ),
)
