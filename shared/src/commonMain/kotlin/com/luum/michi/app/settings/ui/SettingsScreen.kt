package com.luum.michi.app.settings.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.MichiBuildConfig
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.StaffNameLanguage
import com.luum.michi.app.settings.ui.model.StartDestinationChoice
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.domain.model.label
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.OptionList
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.PickerSheet
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.components.AppButton
import com.luum.michi.app.ui.components.Avatar
import com.luum.michi.app.ui.components.ButtonEmphasis
import com.luum.michi.app.ui.components.ToggleRow
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.AppPalette
import com.luum.michi.app.ui.theme.AppPaletteStyle
import com.luum.michi.app.ui.theme.LocalWarning
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.ThemeType
import com.luum.michi.app.ui.theme.displayName
import com.luum.michi.app.ui.theme.fontFamilyFor

/**
 * App settings. Root owns the toolbar (back + title, no actions);
 * this screen never draws its own header. App appearance prefs (language,
 * theme, font, surfaces) live in [settingsState] as local prefs, like the
 * synced ones; only sheet visibility lives here.
 */
@Composable
internal fun SettingsScreen(
    settingsState: SettingsState,
    startDestination: StartDestinationChoice = StartDestinationChoice(),
    viewer: Viewer?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    bottomPadding: Dp,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    var openSheet: SettingsSheet? by remember { mutableStateOf(null) }
    var signOutConfirmSheet by remember { mutableStateOf(false) }

    val listContent: @Composable () -> Unit = {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                // Clears the floating tab bar / navigation insets, like every
                // other list (never a hardcoded per-feature value).
                bottom = 16.dp + bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsAccountCard(
                    viewer = viewer,
                    strings = strings,
                    onSignIn = onLogin,
                    onAccountSettings = { openSheet = SettingsSheet.Account },
                )
            }
            item {
                OptionGroup(title = strings.settingsGeneralSection) {
                    SettingsRow(
                        label = strings.languageLabel,
                        value = settingsState.language.displayName,
                        onClick = { openSheet = SettingsSheet.Language },
                        divider = false,
                    )
                    SettingsRow(
                        label = strings.settingsThemeTitle,
                        value = "${settingsState.themeType.label(strings)} · ${settingsState.palette.displayName}",
                        onClick = { openSheet = SettingsSheet.Theme },
                    )
                    SettingsRow(
                        label = strings.themeFontSection,
                        value = settingsState.font.displayName,
                        onClick = { openSheet = SettingsSheet.Font },
                    )
                    SettingsRow(
                        label = strings.settingsSurfaceTitle,
                        value = settingsState.surfaces.label(strings),
                        onClick = { openSheet = SettingsSheet.Surface },
                    )
                    SettingsRow(
                        label = strings.settingsStyleTitle,
                        value = settingsState.themeStyle.displayName,
                        onClick = { openSheet = SettingsSheet.Style },
                    )
                    SettingsRow(
                        label = strings.settingsImageQualityTitle,
                        value = imageQualityLabel(settingsState.imageQuality, strings),
                        onClick = { openSheet = SettingsSheet.ImageQuality },
                    )
                    SettingsRow(
                        label = strings.settingsStartTabTitle,
                        value = startDestination.tab.label(strings),
                        onClick = { openSheet = SettingsSheet.StartTab },
                    )
                    // Dependent sub-choice: only the tabs that have one show it.
                    startDestination.sectionTitle?.let { sectionTitle ->
                        SettingsRow(
                            label = sectionTitle,
                            value = startDestination.sectionChoices
                                .firstOrNull { it.value == startDestination.sectionValue }
                                ?.label
                                .orEmpty(),
                            onClick = { openSheet = SettingsSheet.StartSection },
                        )
                    }
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
                            onClick = { openSheet = SettingsSheet.TitleLanguage },
                            divider = false,
                        )
                        SettingsRow(
                            label = strings.settingsStaffNameTitle,
                            value = settingsState.staffNameLanguage.label(strings),
                            onClick = { openSheet = SettingsSheet.StaffNameLanguage },
                        )
                        ToggleRow(
                            title = strings.settingsAdultContentTitle,
                            subtitle = strings.settingsAdultContentSubtitle,
                            checked = settingsState.displayAdultContent,
                            onCheckedChange = { settingsState.displayAdultContent = it },
                        )
                        SettingsRow(
                            label = strings.settingsScoreFormatTitle,
                            value = settingsState.scoreFormat.label(strings),
                            onClick = { openSheet = SettingsSheet.ScoreFormat },
                        )
                    }
                }
                item {
                    OptionGroup(title = strings.settingsListsSection) {
                        SettingsRow(
                            label = strings.settingsListSortTitle,
                            value = settingsState.listSort.label(strings),
                            onClick = { openSheet = SettingsSheet.Sort },
                            divider = false,
                        )
                        ToggleRow(
                            title = strings.settingsSplitCompletedAnimeTitle,
                            subtitle = strings.settingsSplitCompletedAnimeSubtitle,
                            checked = settingsState.splitCompletedAnime,
                            onCheckedChange = { settingsState.splitCompletedAnime = it },
                        )
                        ToggleRow(
                            title = strings.settingsSplitCompletedMangaTitle,
                            subtitle = strings.settingsSplitCompletedMangaSubtitle,
                            checked = settingsState.splitCompletedManga,
                            onCheckedChange = { settingsState.splitCompletedManga = it },
                        )
                        ToggleRow(
                            title = strings.settingsAdvancedScoringTitle,
                            subtitle = strings.settingsAdvancedScoringSubtitle,
                            checked = settingsState.advancedScoring,
                            onCheckedChange = { settingsState.advancedScoring = it },
                        )
                        ToggleRow(
                            title = strings.settingsPersistSortTitle,
                            subtitle = strings.settingsPersistSortSubtitle,
                            checked = settingsState.persistListSort,
                            onCheckedChange = { settingsState.persistListSort = it },
                        )
                    }
                }
                item {
                    OptionGroup(title = strings.notificationsSection) {
                        ToggleRow(
                            title = strings.notificationsAiringTitle,
                            subtitle = null,
                            checked = settingsState.notifications.airing,
                            onCheckedChange = {
                                settingsState.notifications =
                                    settingsState.notifications.copy(airing = it)
                            },
                            divider = false,
                        )
                        ToggleRow(
                            title = strings.notificationsMessagesTitle,
                            subtitle = null,
                            checked = settingsState.notifications.messages,
                            onCheckedChange = {
                                settingsState.notifications =
                                    settingsState.notifications.copy(messages = it)
                            },
                        )
                        ToggleRow(
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
                OptionGroup(title = strings.settingsInformationSection) {
                    SettingsRow(
                        label = strings.settingsAboutVersionLabel,
                        value = MichiBuildConfig.Version,
                        onClick = null,
                        divider = false,
                    )
                    SettingsRow(
                        label = strings.settingsAboutTitle,
                        value = strings.settingsAboutSubtitle,
                        onClick = { openSheet = SettingsSheet.About },
                    )
                }
            }
        }
    }

    // Pull-to-refresh only when sync means something: guests are
    // local-only, and a dead pull with no feedback is worse than none.
    // Sheets stay outside the refresh box (modal overlays, no gesture share).
    if (settingsState.canSync) {
        PullRefresh(
            isRefreshing = settingsState.isRefreshing,
            onRefresh = { settingsState.refresh(force = true) },
        ) {
            listContent()
        }
    } else {
        listContent()
    }

    if (openSheet == SettingsSheet.Language) {
        PickerSheet(
            title = strings.languageLabel,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = AppLanguage.available,
            selected = settingsState.language,
            label = { it.displayName },
            onSelect = { settingsState.language = it },
        )
    }

    if (openSheet == SettingsSheet.Theme) {
        ModalSheet(
            title = strings.settingsThemeTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
        ) {
            ThemeModeSection(
                themeType = settingsState.themeType,
                onThemeTypeChange = { settingsState.themeType = it },
                strings = strings,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ThemePaletteSection(
                palette = settingsState.palette,
                onPaletteChange = { settingsState.palette = it },
                strings = strings,
            )
        }
    }

    if (openSheet == SettingsSheet.Font) {
        PickerSheet(
            title = strings.themeFontSection,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = AppFont.entries,
            selected = settingsState.font,
            label = { it.displayName },
            onSelect = { settingsState.font = it },
            leading = { option ->
                Text(
                    text = "Ag",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = fontFamilyFor(option),
                    ),
                )
            },
        )
    }

    if (openSheet == SettingsSheet.Surface) {
        PickerSheet(
            title = strings.settingsSurfaceTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = SurfaceStyle.entries,
            selected = settingsState.surfaces,
            label = { it.label(strings) },
            onSelect = { settingsState.surfaces = it },
        )
    }

    if (openSheet == SettingsSheet.Style) {
        PickerSheet(
            title = strings.settingsStyleTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = AppPaletteStyle.entries,
            selected = settingsState.themeStyle,
            label = { it.displayName },
            onSelect = { settingsState.themeStyle = it },
        )
    }

    if (openSheet == SettingsSheet.ImageQuality) {
        PickerSheet(
            title = strings.settingsImageQualityTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = ImageQuality.entries,
            selected = settingsState.imageQuality,
            label = { imageQualityLabel(it, strings) },
            onSelect = { settingsState.imageQuality = it },
        )
    }

    if (openSheet == SettingsSheet.StartTab) {
        PickerSheet(
            title = strings.settingsStartTabTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = StartTabOption.entries,
            selected = startDestination.tab,
            label = { it.label(strings) },
            onSelect = { settingsState.startTab = it },
        )
    }

    if (openSheet == SettingsSheet.StartSection) {
        val sectionTitle = startDestination.sectionTitle
        val choices = startDestination.sectionChoices
        if (sectionTitle != null && choices.isNotEmpty()) {
            PickerSheet(
                title = sectionTitle,
                dismissLabel = strings.dismissAction,
                onDismiss = { openSheet = null },
                options = choices,
                selected = choices.first(),
                label = { it.label },
                onSelect = { settingsState.setStartSection(it.value) },
                isSelected = { it.value == startDestination.sectionValue },
            )
        }
    }

    if (openSheet == SettingsSheet.TitleLanguage) {
        PickerSheet(
            title = strings.settingsTitleLanguageTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = TitleLanguage.entries,
            selected = settingsState.titleLanguage,
            label = { it.label(strings) },
            onSelect = { settingsState.titleLanguage = it },
        )
    }

    if (openSheet == SettingsSheet.StaffNameLanguage) {
        PickerSheet(
            title = strings.settingsStaffNameTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = StaffNameLanguage.entries,
            selected = settingsState.staffNameLanguage,
            label = { it.label(strings) },
            onSelect = { settingsState.staffNameLanguage = it },
        )
    }

    if (openSheet == SettingsSheet.ScoreFormat) {
        PickerSheet(
            title = strings.settingsScoreFormatTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = ScoreFormat.entries,
            selected = settingsState.scoreFormat,
            label = { it.label(strings) },
            onSelect = { settingsState.scoreFormat = it },
        )
    }

    if (openSheet == SettingsSheet.Sort) {
        PickerSheet(
            title = strings.settingsListSortTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
            options = ListSort.entries,
            selected = settingsState.listSort,
            label = { it.label(strings) },
            onSelect = { settingsState.listSort = it },
        )
    }

    if (openSheet == SettingsSheet.About) {
        ModalSheet(
            title = strings.settingsAboutTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Michi",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${strings.settingsAboutVersionLabel} ${MichiBuildConfig.Version}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = null) {
                Text(
                    text = strings.settingsAboutDescriptionFirst,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.settingsAboutDescriptionSecond,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.settingsAboutDataByText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.settingsAboutUnofficialText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (openSheet == SettingsSheet.Account && viewer != null) {
        ModalSheet(
            title = strings.settingsAccountSettingsTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { openSheet = null },
        ) {
            OptionGroup(title = null) {
                SettingsRow(
                    label = viewer.name,
                    value = strings.settingsSignOutTitle,
                    onClick = { signOutConfirmSheet = true },
                    divider = false,
                    valueColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (signOutConfirmSheet) {
        ModalSheet(
            title = strings.settingsSignOutTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { signOutConfirmSheet = false },
            footer = {
                AppButton(
                    label = strings.settingsSignOutConfirmAction,
                    onClick = {
                        signOutConfirmSheet = false
                        openSheet = null
                        onLogout()
                    },
                    emphasis = ButtonEmphasis.Secondary,
                    containerColor = null,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            },
        ) {
            Text(
                text = strings.settingsSignOutConfirmMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    OptionList(
        options = ThemeType.entries,
        selected = themeType,
        label = { it.label(strings) },
        onSelect = { onThemeTypeChange(it) },
        title = strings.themeModeSection,
        modifier = modifier,
    )
}

@Composable
private fun ThemePaletteSection(
    palette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    OptionList(
        options = AppPalette.entries,
        selected = palette,
        label = { it.displayName },
        onSelect = { onPaletteChange(it) },
        title = strings.themePaletteSection,
        modifier = modifier,
        leading = { option ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(option.seed),
            )
        },
    )
}

/** Session card, first in Settings (identity before prefs). Plain [OptionGroup]:
 * the glass pilot (sep-2026) proved content cards read worse in light mode
 * (translucency washes out, shadow + 40% border render a ghost edge), so
 * glass stays reserved for floating surfaces and actions. */
@Composable
private fun SettingsAccountCard(
    viewer: Viewer?,
    strings: LanguageStrings,
    onSignIn: () -> Unit,
    onAccountSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionGroup(
        title = strings.settingsAccountTitle,
        modifier = modifier,
    ) {
        if (viewer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    imageUrl = viewer.avatarUrl,
                    initials = initialsFor(viewer.name),
                    size = 48.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = viewer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            SettingsRow(
                label = strings.settingsAccountSettingsTitle,
                value = "",
                onClick = onAccountSettings,
            )
        } else {
            SettingsRow(
                label = strings.settingsSignInTitle,
                value = "",
                onClick = onSignIn,
                divider = false,
            )
        }
    }
}

/** Up to two uppercase initials ("Fernando Hurtado" -> "FH", "Michi" -> "M"). */
private fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    val first = parts.first().first().uppercaseChar()
    val second = parts.getOrNull(1)?.first()?.uppercaseChar()
    return if (second != null) "$first$second" else "$first"
}

/** Inline error with retry, shown only when the last load/save failed. Same frame
 * language as [OptionGroup] ([PanelShape] + 1dp border) with the border in error
 * and no fill, so a failed sync reads as an error while aligning with the groups. */
@Composable
private fun SettingsErrorRow(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole card retries (like group rows): Surface carries the tap with
    // its shape-bound ripple, the inner retry row is static layout.
    Surface(
        onClick = onRetry,
        modifier = modifier.fillMaxWidth(),
        shape = PanelShape,
        border = BorderStroke(
            width = 1.dp,
            color = LocalWarning.current,
        ),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Warning,
                contentDescription = null,
                tint = LocalWarning.current,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalWarning.current,
                modifier = Modifier.weight(1f),
            )
            // Same metrics as SettingsRow (12dp vertical, 4dp gap, 20dp
            // chevron): a TextButton carries its own 12dp content padding
            // and 48dp floor, so its chevron can never sit where the
            // groups' chevrons sit.
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = retryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalWarning.current,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = LocalWarning.current,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** The one settings sheet open at a time; null = none. The sign-out confirm
 *  stacks on the account sheet through its own flag. */
private enum class SettingsSheet {
    Language,
    Theme,
    Font,
    Surface,
    Style,
    ImageQuality,
    StartTab,
    StartSection,
    TitleLanguage,
    StaffNameLanguage,
    ScoreFormat,
    Sort,
    About,
    Account,
}

private fun ThemeType.label(strings: LanguageStrings): String = when (this) {
    ThemeType.SYSTEM -> strings.settingsThemeSystem
    ThemeType.LIGHT -> strings.settingsThemeLight
    ThemeType.DARK -> strings.settingsThemeDark
}

private fun SurfaceStyle.label(strings: LanguageStrings): String = when (this) {
    SurfaceStyle.GLASS -> strings.surfaceGlassLabel
    SurfaceStyle.SOLID -> strings.surfaceSolidLabel
}


private fun imageQualityLabel(quality: ImageQuality, strings: LanguageStrings): String = when (quality) {
    ImageQuality.AUTO -> strings.settingsImageQualityAuto
    ImageQuality.HIGH -> strings.settingsImageQualityHigh
    ImageQuality.LOW -> strings.settingsImageQualityLow
}
