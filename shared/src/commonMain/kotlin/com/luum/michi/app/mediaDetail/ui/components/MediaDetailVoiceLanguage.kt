package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.PickerSheet
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
 * The actor's AniList `languageV2` as the picker's localized label ("Japanese"
 * → "Japonés"). Null or blank stays null; a language outside the picker falls
 * through raw so it is never hidden.
 */
internal fun voiceLanguageLabel(value: String?, strings: LanguageStrings): String? {
    val language = value?.takeIf { it.isNotBlank() } ?: return null
    return voiceLanguageOptions(strings)
        .firstOrNull { it.value.equals(language, ignoreCase = true) }
        ?.label
        ?: language
}

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

/** Single-select voice-language sheet (the shared [PickerSheet] with an [OptionList]). */
@Composable
internal fun MediaDetailVoiceLanguageSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    strings: LanguageStrings,
) {
    val options = voiceLanguageOptions(strings)
    PickerSheet(
        title = strings.mediaDetailVoiceLanguageLabel,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        options = options,
        selected = options.firstOrNull { it.value == selected } ?: options.first(),
        label = { it.label },
        onSelect = { onSelect(it.value) },
    )
}
