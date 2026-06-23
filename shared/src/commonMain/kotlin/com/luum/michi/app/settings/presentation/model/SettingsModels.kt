package com.luum.michi.app.settings.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons

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
                icon = { PlatformIcons.Accessibility },
                type = SettingsItemType.THEME,
            ),
            SettingsItem(
                title = strings.settingsLanguageTitle,
                subtitle = strings.settingsLanguageSubtitle,
                icon = { PlatformIcons.Language },
                type = SettingsItemType.LANGUAGE,
            ),
            SettingsItem(
                title = strings.settingsHomeTabTitle,
                subtitle = strings.settingsHomeTabSubtitle,
                icon = { PlatformIcons.Home },
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
                icon = { PlatformIcons.Language },
                type = SettingsItemType.TITLE_LANGUAGE,
            ),
            SettingsItem(
                title = strings.settingsAdultContentTitle,
                subtitle = strings.settingsAdultContentSubtitle,
                icon = { PlatformIcons.Privacy },
                type = SettingsItemType.ADULT_CONTENT,
            ),
            SettingsItem(
                title = strings.settingsScoreFormatTitle,
                subtitle = strings.settingsScoreFormatSubtitle,
                icon = { PlatformIcons.Like },
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
                icon = { PlatformIcons.FilterList },
                type = SettingsItemType.LIST_SORT,
            ),
            SettingsItem(
                title = strings.settingsSplitCompletedAnimeTitle,
                subtitle = strings.settingsSplitCompletedAnimeSubtitle,
                icon = { PlatformIcons.Animation },
                type = SettingsItemType.SPLIT_COMPLETED_ANIME,
            ),
            SettingsItem(
                title = strings.settingsSplitCompletedMangaTitle,
                subtitle = strings.settingsSplitCompletedMangaSubtitle,
                icon = { PlatformIcons.Reading },
                type = SettingsItemType.SPLIT_COMPLETED_MANGA,
            ),
            SettingsItem(
                title = strings.settingsAdvancedScoringTitle,
                subtitle = strings.settingsAdvancedScoringSubtitle,
                icon = { PlatformIcons.ContentPreferences },
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
                icon = { PlatformIcons.Notifications },
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
                icon = { PlatformIcons.ManageAccount },
                type = SettingsItemType.MANAGE_ACCOUNT,
            ),
            SettingsItem(
                title = strings.logoutAction,
                icon = { PlatformIcons.Logout },
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
                icon = { PlatformIcons.HelpSupport },
                type = SettingsItemType.HELP,
            ),
            SettingsItem(
                title = strings.settingsAboutTitle,
                subtitle = strings.settingsAboutSubtitle,
                icon = { PlatformIcons.Information },
                type = SettingsItemType.ABOUT,
            ),
        ),
    ),
)
