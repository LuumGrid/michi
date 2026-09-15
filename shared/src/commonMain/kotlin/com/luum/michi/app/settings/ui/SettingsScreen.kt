package com.luum.michi.app.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luum.michi.app.MichiBuildConfig
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.domain.model.label
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemePalettes
import com.luum.michi.app.ui.theme.ThemeType
import com.luum.michi.app.ui.theme.displayName
import com.luum.michi.app.ui.theme.fontFamilyFor
import com.luum.michi.app.ui.theme.paletteDisplayName

/**
 * App settings. Root owns the toolbar (back + title, no actions);
 * this screen never draws its own header. App prefs are hoisted to `App`
 * (same pattern as the auth landing) and synced prefs live in
 * [settingsState]; only sheet visibility lives here.
 */
@Composable
internal fun SettingsScreen(
    settingsState: SettingsState,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    palette: ThemeColors,
    onPaletteChange: (ThemeColors) -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    var languageSheet by remember { mutableStateOf(false) }
    var themeSheet by remember { mutableStateOf(false) }
    var fontSheet by remember { mutableStateOf(false) }
    var titleLanguageSheet by remember { mutableStateOf(false) }
    var scoreFormatSheet by remember { mutableStateOf(false) }
    var sortSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OptionGroup(title = strings.settingsGeneralSection) {
                SettingsRow(
                    label = strings.languageLabel,
                    value = language.displayName,
                    onClick = { languageSheet = true },
                    divider = false,
                )
                SettingsRow(
                    label = strings.settingsThemeTitle,
                    value = "${themeType.label(strings)} · ${paletteDisplayName(palette)}",
                    onClick = { themeSheet = true },
                )
                SettingsRow(
                    label = strings.themeFontSection,
                    value = font.displayName,
                    onClick = { fontSheet = true },
                )
            }
        }
        if (settingsState.canSync) {
            val error = settingsState.error
            if (error != null) {
                item {
                    SettingsErrorRow(
                        message = strings.networkErrorMessage(error),
                        retryLabel = strings.retryAction,
                        onRetry = { settingsState.refresh(force = true) },
                    )
                }
            }
            item {
                OptionGroup(title = strings.settingsAniListSection) {
                    SettingsRow(
                        label = strings.settingsTitleLanguageTitle,
                        value = settingsState.titleLanguage.label(strings),
                        onClick = { titleLanguageSheet = true },
                        divider = false,
                    )
                    SettingsToggleRow(
                        title = strings.settingsAdultContentTitle,
                        subtitle = strings.settingsAdultContentSubtitle,
                        checked = settingsState.displayAdultContent,
                        onCheckedChange = { settingsState.displayAdultContent = it },
                    )
                    SettingsRow(
                        label = strings.settingsScoreFormatTitle,
                        value = settingsState.scoreFormat.label(strings),
                        onClick = { scoreFormatSheet = true },
                    )
                }
            }
            item {
                OptionGroup(title = strings.settingsListsSection) {
                    SettingsRow(
                        label = strings.settingsListSortTitle,
                        value = settingsState.listSort.label(strings),
                        onClick = { sortSheet = true },
                        divider = false,
                    )
                    SettingsToggleRow(
                        title = strings.settingsSplitCompletedAnimeTitle,
                        subtitle = strings.settingsSplitCompletedAnimeSubtitle,
                        checked = settingsState.splitCompletedAnime,
                        onCheckedChange = { settingsState.splitCompletedAnime = it },
                    )
                    SettingsToggleRow(
                        title = strings.settingsSplitCompletedMangaTitle,
                        subtitle = strings.settingsSplitCompletedMangaSubtitle,
                        checked = settingsState.splitCompletedManga,
                        onCheckedChange = { settingsState.splitCompletedManga = it },
                    )
                    SettingsToggleRow(
                        title = strings.settingsAdvancedScoringTitle,
                        subtitle = strings.settingsAdvancedScoringSubtitle,
                        checked = settingsState.advancedScoring,
                        onCheckedChange = { settingsState.advancedScoring = it },
                    )
                }
            }
            item {
                OptionGroup(title = strings.notificationsSection) {
                    SettingsToggleRow(
                        title = strings.notificationsAiringTitle,
                        subtitle = null,
                        checked = settingsState.notifications.airing,
                        onCheckedChange = {
                            settingsState.notifications =
                                settingsState.notifications.copy(airing = it)
                        },
                        divider = false,
                    )
                    SettingsToggleRow(
                        title = strings.notificationsMessagesTitle,
                        subtitle = null,
                        checked = settingsState.notifications.messages,
                        onCheckedChange = {
                            settingsState.notifications =
                                settingsState.notifications.copy(messages = it)
                        },
                    )
                    SettingsToggleRow(
                        title = strings.notificationsMediaTitle,
                        subtitle = null,
                        checked = settingsState.notifications.media,
                        onCheckedChange = {
                            settingsState.notifications =
                                settingsState.notifications.copy(media = it)
                        },
                    )
                }
            }
        }
        item {
            OptionGroup(title = strings.settingsAboutSection) {
                SettingsRow(
                    label = strings.settingsAboutVersionLabel,
                    value = MichiBuildConfig.Version,
                    onClick = null,
                    divider = false,
                )
                SettingsInfoRow(text = strings.settingsAboutDataByText)
                SettingsInfoRow(text = strings.settingsAboutUnofficialText)
            }
        }
    }

    if (languageSheet) {
        ModalSheet(
            title = strings.languageLabel,
            dismissLabel = strings.dismissAction,
            onDismiss = { languageSheet = false },
        ) {
            OptionGroup(title = null) {
                AppLanguage.available.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.displayName,
                        selected = option == language,
                        onClick = {
                            onLanguageChange(option)
                        },
                        divider = index > 0,
                    )
                }
            }
        }
    }

    if (themeSheet) {
        ModalSheet(
            title = strings.settingsThemeTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { themeSheet = false },
        ) {
            ThemeModeSection(
                themeType = themeType,
                onThemeTypeChange = onThemeTypeChange,
                strings = strings,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ThemePaletteSection(
                palette = palette,
                onPaletteChange = onPaletteChange,
                strings = strings,
            )
        }
    }

    if (fontSheet) {
        ModalSheet(
            title = strings.themeFontSection,
            dismissLabel = strings.dismissAction,
            onDismiss = { fontSheet = false },
        ) {
            OptionGroup(title = null) {
                AppFont.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.displayName,
                        selected = option == font,
                        onClick = {
                            onFontChange(option)
                        },
                        divider = index > 0,
                        leading = {
                            Text(
                                text = "Ag",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = fontFamilyFor(option),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    if (titleLanguageSheet) {
        ModalSheet(
            title = strings.settingsTitleLanguageTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { titleLanguageSheet = false },
        ) {
            OptionGroup(title = null) {
                TitleLanguage.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.label(strings),
                        selected = option == settingsState.titleLanguage,
                        onClick = {
                            settingsState.titleLanguage = option
                        },
                        divider = index > 0,
                    )
                }
            }
        }
    }

    if (scoreFormatSheet) {
        ModalSheet(
            title = strings.settingsScoreFormatTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { scoreFormatSheet = false },
        ) {
            OptionGroup(title = null) {
                ScoreFormat.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.label(strings),
                        selected = option == settingsState.scoreFormat,
                        onClick = {
                            settingsState.scoreFormat = option
                        },
                        divider = index > 0,
                    )
                }
            }
        }
    }

    if (sortSheet) {
        ModalSheet(
            title = strings.settingsListSortTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { sortSheet = false },
        ) {
            OptionGroup(title = null) {
                ListSort.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.label(strings),
                        selected = option == settingsState.listSort,
                        onClick = {
                            settingsState.listSort = option
                        },
                        divider = index > 0,
                    )
                }
            }
        }
    }
}

/** Mode group first: the frequent change sits on top, no scroll needed. */
@Composable
private fun ThemeModeSection(
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    OptionGroup(
        title = strings.themeModeSection,
        modifier = modifier,
    ) {
        ThemeType.entries.forEachIndexed { index, option ->
            OptionRow(
                label = option.label(strings),
                selected = option == themeType,
                onClick = {
                    onThemeTypeChange(option)
                },
                divider = index > 0,
            )
        }
    }
}

@Composable
private fun ThemePaletteSection(
    palette: ThemeColors,
    onPaletteChange: (ThemeColors) -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    OptionGroup(
        title = strings.themePaletteSection,
        modifier = modifier,
    ) {
        ThemePalettes.forEachIndexed { index, (name, option) ->
            OptionRow(
                label = name,
                selected = option::class == palette::class,
                onClick = {
                    onPaletteChange(option)
                },
                divider = index > 0,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(option.seed),
                    )
                },
            )
        }
    }
}

/** Inline toggle row: title + optional subtitle with a trailing Switch. */
@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

/** Inline error with retry, shown only when the last load/save failed. */
@Composable
private fun SettingsErrorRow(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(text = retryLabel)
        }
    }
}

/** Static info line inside a group frame: no value, no interaction. */
@Composable
private fun SettingsInfoRow(
    text: String,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

/** Single settings row: label + current value, opens its picker sheet (or static when onClick is null). */
@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ThemeType.label(strings: LanguageStrings): String = when (this) {
    ThemeType.SYSTEM -> strings.settingsThemeSystem
    ThemeType.LIGHT -> strings.settingsThemeLight
    ThemeType.DARK -> strings.settingsThemeDark
}
