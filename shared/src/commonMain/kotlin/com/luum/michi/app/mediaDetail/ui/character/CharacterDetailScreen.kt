package com.luum.michi.app.mediaDetail.ui.character

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaSort
import com.luum.michi.app.mediaDetail.domain.character.model.label
import com.luum.michi.app.mediaDetail.domain.character.model.sortedFor
import com.luum.michi.app.mediaDetail.ui.character.state.CharacterDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailChipRow
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPanel
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailPersonHero
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailSectionHeader
import com.luum.michi.app.mediaDetail.ui.components.MediaRail
import com.luum.michi.app.mediaDetail.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.language.Strings

/**
 * Character detail page: person hero + person chips + description +
 * information panel + voice actors + appearances rail with its own local
 * sort. Reached from a media character rail via
 * [com.luum.michi.app.root.state.State.openCharacter].
 */
@Composable
internal fun CharacterDetailScreen(
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = holder.detail
    when {
        detail == null && holder.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.mediaDetailLoadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        detail == null && holder.error != null -> {
            MessagePanel(
                title = strings.mediaDetailErrorLabel,
                message = holder.error?.let { strings.networkErrorMessage(it) },
                icon = Icons.Filled.Warning,
                actionLabel = strings.retryAction,
                onAction = onRetry,
                modifier = modifier,
            )
        }
        detail != null -> {
            PullRefresh(isRefreshing = holder.isLoading, onRefresh = onRetry) {
                CharacterDetailContent(
                    detail = detail,
                    holder = holder,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onOpenMedia = onOpenMedia,
                    modifier = modifier,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "hero") {
            MediaDetailPersonHero(
                name = detail.name,
                nativeName = detail.nativeName,
                imageUrl = detail.imageUrl,
                favourites = detail.favourites,
                strings = strings,
            )
        }
        val chips = characterChips(detail, strings)
        if (chips.isNotEmpty()) {
            item(key = "chips") { MediaDetailChipRow(chips = chips) }
        }
        if (detail.descriptionPlain.isNotBlank()) {
            item(key = "description") {
                PersonDescription(text = detail.descriptionPlain, strings = strings)
            }
        }
        item(key = "information") {
            CharacterInformationPanel(detail = detail, strings = strings)
        }
        val voiceActors = holder.mediaItems.distinctVoiceActors()
        if (voiceActors.isNotEmpty()) {
            item(key = "voice-actors") {
                VoiceActorsSection(voiceActors = voiceActors, strings = strings)
            }
        }
        item(key = "media") {
            AppearancesSection(holder = holder, strings = strings, onOpenMedia = onOpenMedia)
        }
    }
}

/** Favourites + the primary roles this character appears in. */
private fun characterChips(detail: CharacterDetail, strings: LanguageStrings): List<String> =
    buildList {
        detail.favourites?.let { add("$it ${strings.favoritesLabel}") }
        detail.media.items
            .mapNotNull { characterRoleLabel(it.role, strings) }
            .distinct()
            .forEach { add(it) }
    }

@Composable
private fun CharacterInformationPanel(detail: CharacterDetail, strings: LanguageStrings) {
    val rows = buildList {
        detail.nativeName?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailJapaneseLabel to it) }
        detail.gender?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailGenderLabel to it) }
        detail.age?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailAgeLabel to it) }
        detail.bloodType?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBloodTypeLabel to it) }
        detail.birthday?.takeIf { it.isNotBlank() }?.let { add(strings.mediaDetailBirthdayLabel to it) }
        detail.alternativeNames
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { add(strings.mediaDetailSynonymsLabel to it.joinToString(", ")) }
        detail.favourites?.let { add(strings.favoritesLabel to it.toString()) }
    }
    MediaDetailPanel {
        MediaDetailSectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { (label, value) -> InfoRow(label = label, value = value) }
    }
}

@Composable
private fun VoiceActorsSection(voiceActors: List<CharacterMediaItem>, strings: LanguageStrings) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailVoiceLanguageLabel,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        MediaRail(
            title = null,
            items = voiceActors.map { item ->
                MediaRailItem(
                    id = item.voiceActorId ?: item.mediaId,
                    title = item.voiceActorName.orEmpty(),
                    imageUrl = item.voiceActorImageUrl,
                    subtitle = voiceLanguageLabel(item.voiceActorLanguage, strings),
                )
            },
            onSelect = null,
        )
    }
}

@Composable
private fun AppearancesSection(
    holder: CharacterDetailStateHolder,
    strings: LanguageStrings,
    onOpenMedia: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MediaDetailSectionHeader(
            title = strings.mediaDetailAppearancesTitle,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChipRail(
            tabs = CharacterMediaSort.entries.map { ChipTab(value = it, label = it.label(strings)) },
            selected = holder.sort,
            onSelect = holder::changeSort,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (holder.mediaItems.isEmpty() && !holder.isLoading) {
            Text(
                text = strings.mediaDetailNoMediaLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            MediaRail(
                title = null,
                items = holder.mediaItems.sortedFor(holder.sort).map { item ->
                    MediaRailItem(
                        id = item.mediaId,
                        title = item.title,
                        imageUrl = item.coverUrl,
                        subtitle = listOfNotNull(
                            characterRoleLabel(item.role, strings),
                            item.voiceActorName,
                        ).joinToString(" · ").takeIf { it.isNotBlank() },
                    )
                },
                onSelect = onOpenMedia,
                onLoadMore = if (holder.hasNextPage && !holder.isLoadingMore) {
                    { holder.loadMore() }
                } else {
                    null
                },
                loadMoreLabel = strings.mediaDetailLoadMoreAction,
            )
        }
    }
}

/** Localized role for the chip/rail subtitle; unknown values fall through. */
private fun characterRoleLabel(role: String?, strings: LanguageStrings): String? = when (role) {
    "MAIN" -> strings.mediaDetailCharacterRoleMain
    "SUPPORTING" -> strings.mediaDetailCharacterRoleSupporting
    "BACKGROUND" -> strings.mediaDetailCharacterRoleBackground
    else -> null
}

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

@Composable
private fun PersonDescription(text: String, strings: LanguageStrings) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = strings.mediaDetailDescriptionTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
