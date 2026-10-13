package com.luum.michi.app.mediaDetail.ui.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.ui.character.state.CharacterDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailExpandableText
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoRow
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailStateContainer
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailVoiceLanguagePill
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailVoiceLanguageSheet
import com.luum.michi.app.mediaDetail.ui.components.voiceLanguageOptions
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.mediaDetail.ui.components.characterRoleLabel
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.language.Strings

/**
 * Character detail page: person hero + framed roles chips + description +
 * information panel + voice actors (with their language picker) + appearances
 * rail with its own local sort. Reached from a media character rail via
 * [com.luum.michi.app.root.state.State.openCharacter].
 */
@Composable
internal fun CharacterDetailScreen(
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    MediaDetailStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        error = holder.error,
        strings = strings,
        onRetry = onRetry,
        modifier = modifier,
    ) {
        if (detail != null) {
            CharacterDetailContent(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
                onOpenMedia = onOpenMedia,
                onOpenStaff = onOpenStaff,
                onViewAll = onViewAll,
            )
        }
    }
}

@Composable
private fun CharacterDetailContent(
    detail: CharacterDetail,
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenMedia: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVoiceLanguageOpen by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "hero") {
            MediaDetailPersonHero(
                name = detail.name,
                nativeName = detail.nativeName,
                image = detail.image,
                favourites = detail.favourites,
                strings = strings,
            )
        }
        val chips = characterChips(detail, strings)
        if (chips.isNotEmpty()) {
            item(key = "roles") {
                MediaDetailPanel {
                    SectionHeader(strings.mediaDetailRolesTitle)
                    Spacer(modifier = Modifier.height(12.dp))
                    ChipRail(
                        tabs = chips.map { ChipTab(value = it, label = it) },
                        selected = null,
                        onSelect = {},
                        contentPadding = PaddingValues(0.dp),
                    )
                }
            }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "description") {
                MediaDetailExpandableText(
                    title = strings.mediaDetailDescriptionTitle,
                    text = detail.descriptionPlain,
                    strings = strings,
                )
            }
        }
        item(key = "information") {
            CharacterInformationPanel(detail = detail, strings = strings)
        }
        // Kept visible while the character has media, so the language picker
        // stays reachable even when the chosen language has no cast.
        if (holder.mediaItems.isNotEmpty()) {
            item(key = "voice-actors") {
                VoiceActorsSection(
                    voiceActors = holder.mediaItems.distinctVoiceActors(),
                    strings = strings,
                    language = holder.voiceLanguage,
                    onOpenLanguagePicker = { isVoiceLanguageOpen = true },
                    onOpenStaff = onOpenStaff,
                )
            }
        }
        item(key = "media") {
            AppearancesSection(
                holder = holder,
                strings = strings,
                onOpenMedia = onOpenMedia,
                onViewAll = { onViewAll(DetailListKind.CHARACTER_MEDIA) },
            )
        }
    }
    if (isVoiceLanguageOpen) {
        MediaDetailVoiceLanguageSheet(
            selected = holder.voiceLanguage,
            onSelect = {
                holder.selectVoiceLanguage(it)
                isVoiceLanguageOpen = false
            },
            onDismiss = { isVoiceLanguageOpen = false },
            strings = strings,
        )
    }
}

/** The primary roles this character appears in (favourites live in the hero). */
private fun characterChips(detail: CharacterDetail, strings: LanguageStrings): List<String> =
    buildList {
        detail.media.items
            .mapNotNull { characterRoleLabel(it.role, strings) }
            .distinct()
            .forEach { add(it) }
    }

@Composable
private fun CharacterInformationPanel(detail: CharacterDetail, strings: LanguageStrings) {
    val rows = buildList {
        detail.gender?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailGenderLabel to it) }
        detail.age?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailAgeLabel to it) }
        detail.bloodType?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBloodTypeLabel to it) }
        detail.birthday?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBirthdayLabel to it) }
        detail.alternativeNames
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(strings.mediaDetailSynonymsLabel to it.joinToString(", ")) }
    }
    if (rows.isEmpty()) return
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { (label, value) -> MediaDetailInfoRow(label = label, value = value) }
    }
}

@Composable
private fun VoiceActorsSection(
    voiceActors: List<CharacterMediaItem>,
    strings: LanguageStrings,
    language: String,
    onOpenLanguagePicker: () -> Unit,
    onOpenStaff: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                title = strings.mediaDetailVoiceActorsTitle,
                modifier = Modifier.weight(1f),
            )
            MediaDetailVoiceLanguagePill(
                label = voiceLanguageOptions(strings)
                    .firstOrNull { it.value == language }
                    ?.label
                    ?: language,
                onClick = onOpenLanguagePicker,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        MediaRail(
            title = null,
            items = voiceActors.mapNotNull { item ->
                // Only actors with an id are reachable; the rest are dropped.
                val actorId = item.voiceActorId ?: return@mapNotNull null
                MediaRailItem(
                    id = actorId,
                    title = item.voiceActorName.orEmpty(),
                    image = item.voiceActorImage,
                    subtitle = voiceLanguageLabel(item.voiceActorLanguage, strings),
                )
            },
            onSelect = onOpenStaff,
            // No cast in the chosen language: say so instead of dropping the
            // whole section (which would hide the language picker).
            emptyLabel = strings.mediaDetailNoVoiceActorsLabel,
        )
    }
}

@Composable
private fun AppearancesSection(
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
    onViewAll: () -> Unit,
) {
    MediaRail(
        title = strings.mediaDetailAppearancesTitle,
        items = holder.mediaItems.take(RailPreviewCount).map { it.toRailItem(strings) },
        onSelect = onOpenMedia,
        onViewAll = onViewAll,
        viewAllLabel = strings.viewAllAction,
        emptyLabel = strings.mediaDetailNoMediaLabel.takeIf { !holder.isLoading },
    )
}

/** Rail preview cap: the rail shows the first N, "View all" shows the rest. */
private const val RailPreviewCount = 10

/** One row per voice actor id (an actor can voice several of the media). */
private fun List<CharacterMediaItem>.distinctVoiceActors(): List<CharacterMediaItem> =
    filter { it.voiceActorId != null }
        .distinctBy { it.voiceActorId }
        .sortedBy { it.voiceActorName.orEmpty().lowercase() }

/** Simple localized label for the raw `languageV2` code; raw as fallback. */
private fun voiceLanguageLabel(raw: String?, strings: LanguageStrings): String? {
    if (raw.isNullOrBlank()) return null
    return when {
        raw.contains("Japanese", ignoreCase = true) -> strings.voiceLanguageJapanese
        raw.contains("English", ignoreCase = true) -> strings.voiceLanguageEnglish
        raw.contains("Korean", ignoreCase = true) -> strings.voiceLanguageKorean
        raw.contains("Italian", ignoreCase = true) -> strings.voiceLanguageItalian
        raw.contains("Spanish", ignoreCase = true) -> strings.voiceLanguageSpanish
        raw.contains("Portuguese", ignoreCase = true) -> strings.voiceLanguagePortuguese
        raw.contains("French", ignoreCase = true) -> strings.voiceLanguageFrench
        raw.contains("German", ignoreCase = true) -> strings.voiceLanguageGerman
        raw.contains("Hebrew", ignoreCase = true) -> strings.voiceLanguageHebrew
        raw.contains("Hungarian", ignoreCase = true) -> strings.voiceLanguageHungarian
        else -> raw
    }
}
