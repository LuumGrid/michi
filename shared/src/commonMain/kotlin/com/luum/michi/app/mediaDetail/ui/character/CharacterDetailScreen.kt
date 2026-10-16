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
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoEntry
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailInfoGrid
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailVoiceLanguagePill
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailVoiceLanguageSheet
import com.luum.michi.app.mediaDetail.ui.components.voiceLanguageOptions
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.mediaDetail.ui.components.characterRoleLabel
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.core.model.RailPageSize
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailLinkSection
import com.luum.michi.app.mediaDetail.ui.components.toLinkItem

/**
 * Character detail page: person hero (portrait + identity, with the roles as
 * chips inside it) + description + information panel + voice actors (with their
 * language picker) + appearances rail. Reached from a media character rail via
 * [com.luum.michi.app.root.state.State.openCharacter].
 */
@Composable
internal fun CharacterDetailScreen(
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onViewAll: (DetailListKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    LoadStateContainer(
        hasContent = detail != null,
        isLoading = holder.isLoading,
        errorMessage = holder.error?.let { strings.networkErrorMessage(it) },
        loadingLabel = strings.mediaDetailLoadingLabel,
        errorTitle = strings.mediaDetailErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = onRetry,
        modifier = modifier,
        refreshError = if (detail != null) holder.error?.let { strings.networkErrorMessage(it) } else null,
        actionError = holder.actionError?.let { strings.networkErrorMessage(it) },
        dismissLabel = strings.dismissAction,
        onDismissActionError = { holder.clearActionError() },
    ) {
        if (detail != null) {
            CharacterDetailContent(
                detail = detail,
                holder = holder,
                strings = strings,
                bottomPadding = bottomPadding,
                onOpenUrl = onOpenUrl,
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
    onOpenUrl: (String) -> Unit,
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
                // The roles live in the hero column, not in their own row.
                chips = characterChips(detail, strings),
            )
        }
        if (detail.descriptionPlain.isNotBlank() || detail.stats.isNotEmpty()) {
            item(key = "description") {
                MediaDetailExpandableText(
                    title = strings.mediaDetailCharacterDescriptionTitle,
                    text = detail.descriptionPlain,
                    strings = strings,
                    stats = detail.stats,
                )
            }
        }
        if (detail.links.isNotEmpty()) {
            item(key = "links") {
                MediaDetailLinkSection(
                    links = detail.links.map { it.toLinkItem(strings) },
                    title = strings.mediaDetailExternalLinksTitle,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
        // The cells are built here so an empty panel is not emitted at all: an
        // item with nothing to paint still leaves its spacing behind.
        val informationEntries = buildList {
            detail.gender?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailGenderLabel, it)) }
            detail.age?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailAgeLabel, it)) }
            detail.bloodType?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailBloodTypeLabel, it)) }
            detail.birthday?.takeIf { it.isNotBlank() }?.let { add(MediaDetailInfoEntry(strings.mediaDetailBirthdayLabel, it)) }
        }
        val informationWideEntries = detail.alternativeNames
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(MediaDetailInfoEntry(strings.mediaDetailAlternativeNamesLabel, it.joinToString(", "))) }
            .orEmpty()
        if (informationEntries.isNotEmpty() || informationWideEntries.isNotEmpty()) {
            item(key = "information") {
                CharacterInformationPanel(
                    entries = informationEntries,
                    wideEntries = informationWideEntries,
                    strings = strings,
                )
            }
        }
        // Kept visible while the character has media, so the language picker
        // stays reachable even when the chosen language has no cast.
        if (holder.media.items.isNotEmpty()) {
            item(key = "voice-actors") {
                VoiceActorsSection(
                    voiceActors = holder.media.items.distinctVoiceActors(),
                    strings = strings,
                    language = holder.voiceLanguage,
                    onOpenLanguagePicker = { isVoiceLanguageOpen = true },
                    onOpenStaff = onOpenStaff,
                )
            }
        }
        // Empty rails are not painted at all (a header with "No media" reads as
        // a broken section).
        if (holder.media.items.isNotEmpty()) {
            item(key = "media") {
                AppearancesSection(
                    holder = holder,
                    strings = strings,
                    onOpenMedia = onOpenMedia,
                    onViewAll = { onViewAll(DetailListKind.CHARACTER_MEDIA) },
                )
            }
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
private fun CharacterInformationPanel(
    entries: List<MediaDetailInfoEntry>,
    wideEntries: List<MediaDetailInfoEntry>,
    strings: LanguageStrings,
) {
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        MediaDetailInfoGrid(entries = entries, wideEntries = wideEntries)
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
                )
            },
            onSelect = onOpenStaff,
            // No cast in the chosen language: say so instead of dropping the
            // whole section (which would hide the language picker).
            emptyLabel = strings.mediaDetailNoVoiceActorsLabel,
            // The item set changes with the language: back to the start.
            resetKey = language,
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
        items = holder.media.items.take(RailPageSize).map { it.toRailItem(strings) },
        onSelect = onOpenMedia,
        onViewAll = onViewAll,
        viewAllLabel = strings.viewAllAction,
    )
}


/** One row per voice actor id (an actor can voice several of the media). */
private fun List<CharacterMediaItem>.distinctVoiceActors(): List<CharacterMediaItem> =
    filter { it.voiceActorId != null }
        .distinctBy { it.voiceActorId }
        .sortedBy { it.voiceActorName.orEmpty().lowercase() }
