package com.luum.michi.app.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
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
 * this screen never draws its own header. Selections are hoisted to `App`
 * (same pattern as the auth landing); only sheet visibility lives here.
 */
@Composable
internal fun SettingsScreen(
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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

/** Single settings row: label + current value, opens its picker sheet. */
@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit,
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
                .clickable(onClick = onClick)
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
