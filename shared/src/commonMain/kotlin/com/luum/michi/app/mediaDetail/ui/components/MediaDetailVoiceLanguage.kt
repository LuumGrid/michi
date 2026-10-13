package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.GlassShape
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.glassContainerColor
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

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
 * Compact trailing control of the characters header: globe + current voice
 * language + chevron. Inline surface, so it takes the glass *recipe*
 * (translucent color + border, no shadow) like the detail pills.
 */
@Composable
internal fun MediaDetailVoiceLanguagePill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Surface(
        onClick = onClick,
        shape = GlassShape,
        color = if (glass) {
            glassContainerColor()
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (glass) glassBorder() else solidBorder(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = AppIcons.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
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
