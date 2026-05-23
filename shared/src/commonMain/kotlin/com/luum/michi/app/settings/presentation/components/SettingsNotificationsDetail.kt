package com.luum.michi.app.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.settings.presentation.model.NotificationPreferences
import com.luum.michi.app.settings.presentation.model.SettingsItem

@Composable
internal fun SettingsNotificationsDetail(
    preferences: NotificationPreferences,
    onChange: (NotificationPreferences) -> Unit,
) {
    val strings = LanguageProvider.strings

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifAiringTitle,
                icon = { PlatformIcons.Animation },
            ),
            checked = preferences.airing,
            onCheckedChange = { onChange(preferences.copy(airing = it)) },
        )
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifActivityTitle,
                icon = { PlatformIcons.UserActivity },
            ),
            checked = preferences.activity,
            onCheckedChange = { onChange(preferences.copy(activity = it)) },
        )
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifFollowingTitle,
                icon = { PlatformIcons.SwitchAccount },
            ),
            checked = preferences.following,
            onCheckedChange = { onChange(preferences.copy(following = it)) },
        )
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifForumTitle,
                icon = { PlatformIcons.Comments },
            ),
            checked = preferences.forum,
            onCheckedChange = { onChange(preferences.copy(forum = it)) },
        )
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifMessagesTitle,
                icon = { PlatformIcons.Mood },
            ),
            checked = preferences.messages,
            onCheckedChange = { onChange(preferences.copy(messages = it)) },
        )
        SettingsToggleRow(
            item = SettingsItem(
                title = strings.settingsNotifMediaTitle,
                icon = { PlatformIcons.ContentPreferences },
            ),
            checked = preferences.media,
            onCheckedChange = { onChange(preferences.copy(media = it)) },
        )
    }
}
