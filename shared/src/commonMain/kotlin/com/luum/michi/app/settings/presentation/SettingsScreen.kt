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
import com.luum.michi.app.settings.presentation.model.SettingsItem
import com.luum.michi.app.settings.presentation.model.SettingsItemType
import com.luum.michi.app.settings.presentation.model.settingsGroups

@Composable
internal fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
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
            top = 112.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        groups.forEach { group ->
            item(key = group.title) {
                SettingsGroupHeader(text = group.title)
            }

            items(group.items, key = { it.title }) { item ->
                SettingsRow(
                    item = item,
                    onClick = when (item.type) {
                        SettingsItemType.ADD_ACCOUNT -> onAddAccount
                        SettingsItemType.LOGOUT -> onLogout
                        else -> ({ selectedItem = item })
                    },
                )
            }
        }
    }
}
