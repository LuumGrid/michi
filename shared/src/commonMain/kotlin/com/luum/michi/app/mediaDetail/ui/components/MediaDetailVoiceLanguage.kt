package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.TrailingPill
import com.luum.michi.app.ui.icons.AppIcons

/** One voice-language choice: the AniList enum value + its localized label. */
internal data class VoiceLanguageOption(val value: String, val label: String)

/** The ten languages the characters query accepts, in picker order. */
internal fun voiceLanguageOptions(strings: LanguageStrings): List<VoiceLanguageOption> = listOf(
    VoiceLanguageOption("JAPANESE", strings.voiceLanguageJapanese),
    VoiceLanguageOption("ENGLISH", strings.voiceLanguageEnglish),
    VoiceLanguageOption("KOREAN", strings.voiceLanguageKorean),
    VoiceLanguageOption("ITALIAN", strings.voiceLanguageItalian),
    VoiceLanguageOption("SPANISH", strings.voiceLanguageSpanish),
    VoiceLanguageOption("PORTUGUESE", strings.voiceLanguagePortuguese),
    VoiceLanguageOption("FRENCH", strings.voiceLanguageFrench),
    VoiceLanguageOption("GERMAN", strings.voiceLanguageGerman),
    VoiceLanguageOption("HEBREW", strings.voiceLanguageHebrew),
    VoiceLanguageOption("HUNGARIAN", strings.voiceLanguageHungarian),
)

/**
 * Compact trailing control of the characters header: the shared [TrailingPill]
 * with a globe and a chevron.
 */
@Composable
internal fun MediaDetailVoiceLanguagePill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrailingPill(
        label = label,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = AppIcons.Language,
        trailingIcon = AppIcons.ExpandMore,
    )
}

/** Single-select voice-language sheet (`OptionGroup` + `OptionRow`, like the settings pickers). */
@Composable
internal fun MediaDetailVoiceLanguageSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    strings: LanguageStrings,
) {
    ModalSheet(
        title = strings.mediaDetailVoiceLanguageLabel,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
    ) {
        OptionGroup(title = null) {
            voiceLanguageOptions(strings).forEachIndexed { index, option ->
                OptionRow(
                    label = option.label,
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    divider = index != 0,
                )
            }
        }
    }
}
