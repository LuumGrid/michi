package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.core.language.domain.LanguageStrings

/**
 * The Anime/Manga selector chip rail, shared by Discover and the genre page.
 * Generic over the catalog enum so it stays primitive-only: each feature keeps
 * its own type and maps it back on select.
 */
@Composable
internal fun <T> CatalogChipRail(
    anime: T,
    manga: T,
    selected: T,
    onSelect: (T) -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    ChipRail(
        tabs = listOf(
            ChipTab(value = anime, label = strings.animeLabel),
            ChipTab(value = manga, label = strings.mangaLabel),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}
