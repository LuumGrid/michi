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
    ADD_ACCOUNT,
    ACCESSIBILITY,
    LANGUAGE,
    LOGOUT,
}

internal data class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: @Composable () -> Painter,
    val type: SettingsItemType = SettingsItemType.DEFAULT,
)

internal fun settingsGroups(strings: LanguageStrings) = listOf(
    SettingsGroup(
        title = strings.settingsAccountSection,
        items = listOf(
            SettingsItem(
                title = strings.logoutAction,
                icon = { PlatformIcons.Logout },
                type = SettingsItemType.LOGOUT,
            ),
            SettingsItem(
                title = strings.addAccountAction,
                icon = { PlatformIcons.SwitchAccount },
                type = SettingsItemType.ADD_ACCOUNT,
            ),
            SettingsItem(
                title = strings.settingsManageAccountTitle,
                subtitle = strings.settingsManageAccountSubtitle,
                icon = { PlatformIcons.ManageAccount },
            ),
            SettingsItem(
                title = strings.settingsPrivacyTitle,
                subtitle = strings.settingsPrivacySubtitle,
                icon = { PlatformIcons.Privacy },
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsContentSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsContentPreferencesTitle,
                subtitle = strings.settingsContentPreferencesSubtitle,
                icon = { PlatformIcons.ContentPreferences },
            ),
            SettingsItem(
                title = strings.settingsHistoryTitle,
                subtitle = strings.settingsHistorySubtitle,
                icon = { PlatformIcons.History },
            ),
            SettingsItem(
                title = strings.settingsInteractionsTitle,
                subtitle = strings.settingsInteractionsSubtitle,
                icon = { PlatformIcons.UserActivity },
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsExperienceSection,
        items = listOf(
            SettingsItem(
                title = strings.notificationsAction,
                subtitle = strings.settingsNotificationsSubtitle,
                icon = { PlatformIcons.Notifications },
            ),
            SettingsItem(
                title = strings.settingsLanguageTitle,
                subtitle = strings.settingsLanguageSubtitle,
                icon = { PlatformIcons.Language },
                type = SettingsItemType.LANGUAGE,
            ),
            SettingsItem(
                title = strings.settingsAccessibilityTitle,
                subtitle = strings.settingsAccessibilitySubtitle,
                icon = { PlatformIcons.Accessibility },
                type = SettingsItemType.ACCESSIBILITY,
            ),
            SettingsItem(
                title = strings.settingsDataPlaybackTitle,
                subtitle = strings.settingsDataPlaybackSubtitle,
                icon = { PlatformIcons.DataPlayback },
            ),
        ),
    ),
    SettingsGroup(
        title = strings.settingsToolsSection,
        items = listOf(
            SettingsItem(
                title = strings.settingsCreatorToolsTitle,
                subtitle = strings.settingsCreatorToolsSubtitle,
                icon = { PlatformIcons.CreatorTools },
            ),
            SettingsItem(
                title = strings.settingsHelpTitle,
                subtitle = strings.settingsHelpSubtitle,
                icon = { PlatformIcons.HelpSupport },
            ),
            SettingsItem(
                title = strings.settingsAboutTitle,
                subtitle = strings.settingsAboutSubtitle,
                icon = { PlatformIcons.Information },
            ),
        ),
    ),
)
