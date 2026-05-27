package com.luum.michi.app.mediaDetail.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

internal val VoiceLanguageOptions = listOf(
    "JAPANESE",
    "ENGLISH",
    "KOREAN",
    "SPANISH",
    "FRENCH",
    "GERMAN",
    "ITALIAN",
    "PORTUGUESE",
    "HEBREW",
    "HUNGARIAN",
)

@Composable
internal fun CharactersTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VoiceLanguageChipRow(
            selected = stateHolder.voiceLanguage,
            onSelect = stateHolder::selectVoiceLanguage,
            strings = strings,
        )
        val characters = stateHolder.characters
        if (characters.isEmpty() && !stateHolder.isLoadingCharacters) {
            PlatformListMessage(
                title = strings.mediaDetailNoCharactersLabel,
                tone = PlatformListMessageTone.Neutral,
            )
            return@Column
        }
        val gridState = rememberLazyGridState()
        LaunchedEffect(gridState, stateHolder.charactersHasNextPage) {
            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible
            }.collect { last ->
                if (last >= characters.size - 4 && stateHolder.charactersHasNextPage && !stateHolder.isLoadingCharacters) {
                    stateHolder.loadMoreCharacters()
                }
            }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            gridItems(items = characters, key = { it.edgeKey }) { entry ->
                CharacterCard(entry = entry, strings = strings)
            }
            if (stateHolder.isLoadingCharacters) {
                item { LoadingTile() }
                item { LoadingTile() }
            }
        }
    }
}

@Composable
internal fun VoiceLanguageChipRow(
    selected: String,
    onSelect: (String) -> Unit,
    strings: LanguageStrings,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = VoiceLanguageOptions, key = { it }) { lang ->
            FilterChip(
                selected = lang == selected,
                onClick = { onSelect(lang) },
                label = { Text(voiceLanguageLabel(lang, strings)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

internal fun voiceLanguageLabel(code: String, strings: LanguageStrings): String = when (code) {
    "JAPANESE" -> strings.voiceLanguageJapanese
    "ENGLISH" -> strings.voiceLanguageEnglish
    "KOREAN" -> strings.voiceLanguageKorean
    "ITALIAN" -> strings.voiceLanguageItalian
    "SPANISH" -> strings.voiceLanguageSpanish
    "PORTUGUESE" -> strings.voiceLanguagePortuguese
    "FRENCH" -> strings.voiceLanguageFrench
    "GERMAN" -> strings.voiceLanguageGerman
    "HEBREW" -> strings.voiceLanguageHebrew
    "HUNGARIAN" -> strings.voiceLanguageHungarian
    else -> code
}

@Composable
internal fun CharacterCard(entry: MediaCharacterEntry, strings: LanguageStrings) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PersonHalf(
                imageUrl = entry.imageUrl,
                name = entry.name,
                subtitle = roleLabel(entry.role, strings),
                modifier = Modifier.weight(1f),
            )
            entry.voiceActor?.let { va ->
                PersonHalf(
                    imageUrl = va.imageUrl,
                    name = va.name,
                    subtitle = va.language ?: "",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun PersonHalf(
    imageUrl: String?,
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun roleLabel(role: MediaCharacterRole, strings: LanguageStrings): String = when (role) {
    MediaCharacterRole.MAIN -> strings.mediaDetailCharacterRoleMain
    MediaCharacterRole.SUPPORTING -> strings.mediaDetailCharacterRoleSupporting
    MediaCharacterRole.BACKGROUND -> strings.mediaDetailCharacterRoleBackground
    MediaCharacterRole.OTHER -> ""
}
