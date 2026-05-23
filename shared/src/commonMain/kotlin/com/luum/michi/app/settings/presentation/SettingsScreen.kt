package com.luum.michi.app.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformBackHandlerSetter
import com.luum.michi.app.settings.presentation.components.SettingsDetailContent
import com.luum.michi.app.settings.presentation.components.SettingsGroupHeader
import com.luum.michi.app.settings.presentation.components.SettingsRow
import com.luum.michi.app.settings.presentation.components.SettingsToggleRow
import com.luum.michi.app.settings.presentation.model.SettingsItem
import com.luum.michi.app.settings.presentation.model.SettingsItemType
import com.luum.michi.app.settings.presentation.model.isAction
import com.luum.michi.app.settings.presentation.model.isInlineToggle
import com.luum.michi.app.settings.presentation.model.settingsGroups
import com.luum.michi.app.settings.presentation.state.SettingsState

@Composable
internal fun SettingsScreen(
    settingsState: SettingsState,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onManageAccount: () -> Unit,
    onHelp: () -> Unit,
    onBackHandlerChange: PlatformBackHandlerSetter,
) {
    val strings = LanguageProvider.strings
    val groups = remember(strings) { settingsGroups(strings) }
    var selectedItem by remember { mutableStateOf<SettingsItem?>(null) }

    DisposableEffect(selectedItem) {
        onBackHandlerChange(
            if (selectedItem != null) {
                { selectedItem = null }
            } else {
                null
            },
        )
        onDispose { onBackHandlerChange(null) }
    }

    val detailItem = selectedItem
    if (detailItem != null) {
        SettingsDetailContent(
            item = detailItem,
            settingsState = settingsState,
            language = language,
            onLanguageChange = onLanguageChange,
            isDarkMode = isDarkMode,
            onToggleTheme = onToggleTheme,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        groups.forEach { group ->
            item(key = "header_${group.title}") {
                SettingsGroupHeader(text = group.title)
            }

            items(group.items, key = { it.type.name }) { item ->
                SettingsItemRow(
                    item = item,
                    settingsState = settingsState,
                    onOpenDetail = { selectedItem = item },
                    onAddAccount = onAddAccount,
                    onLogout = onLogout,
                    onManageAccount = onManageAccount,
                    onHelp = onHelp,
                )
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    settingsState: SettingsState,
    onOpenDetail: () -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onManageAccount: () -> Unit,
    onHelp: () -> Unit,
) {
    when {
        item.type.isInlineToggle -> {
            val checked = when (item.type) {
                SettingsItemType.ADULT_CONTENT -> settingsState.displayAdultContent
                SettingsItemType.SPLIT_COMPLETED_ANIME -> settingsState.splitCompletedAnime
                SettingsItemType.SPLIT_COMPLETED_MANGA -> settingsState.splitCompletedManga
                SettingsItemType.ADVANCED_SCORING -> settingsState.advancedScoring
                else -> false
            }
            val onCheckedChange: (Boolean) -> Unit = { next ->
                when (item.type) {
                    SettingsItemType.ADULT_CONTENT -> settingsState.displayAdultContent = next
                    SettingsItemType.SPLIT_COMPLETED_ANIME -> settingsState.splitCompletedAnime = next
                    SettingsItemType.SPLIT_COMPLETED_MANGA -> settingsState.splitCompletedManga = next
                    SettingsItemType.ADVANCED_SCORING -> settingsState.advancedScoring = next
                    else -> Unit
                }
            }
            SettingsToggleRow(item = item, checked = checked, onCheckedChange = onCheckedChange)
        }

        item.type.isAction -> {
            val onClick = when (item.type) {
                SettingsItemType.ADD_ACCOUNT -> onAddAccount
                SettingsItemType.LOGOUT -> onLogout
                SettingsItemType.MANAGE_ACCOUNT -> onManageAccount
                SettingsItemType.HELP -> onHelp
                else -> ({ })
            }
            SettingsRow(item = item, onClick = onClick)
        }

        else -> SettingsRow(item = item, onClick = onOpenDetail)
    }
}
