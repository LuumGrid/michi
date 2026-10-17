package com.luum.michi.app.mediaList.ui.anime

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.catalogMetaLine
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.animeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.common.mediaListFormatOptions
import com.luum.michi.app.mediaList.ui.anime.components.AnimeListEntryContent
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.common.MediaListScreenScaffold
import com.luum.michi.app.mediaList.ui.common.MediaListScreenSettings
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Anime list: thin wrapper over [MediaListScreenScaffold] that fills only what
 * differs — sections, empty icon, meta line, format options and the entry body.
 */
@Composable
internal fun AnimeListScreen(
    holder: AnimeListStateHolder,
    selected: AnimeListSection,
    onSelectSection: (AnimeListSection) -> Unit,
    onReload: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last item clears the floating
    // TabBar with the same 16dp rhythm the first item keeps under the rail.
    bottomPadding: Dp = 0.dp,
    isRefreshing: Boolean = false,
    settings: MediaListScreenSettings = MediaListScreenSettings(),
    // List tools (toolbar filter/sort): Root owns the open flags so the
    // toolbar back affordance and system back close the sheets.
    isFilterOpen: Boolean = false,
    onDismissFilter: () -> Unit = {},
    isSortOpen: Boolean = false,
    onDismissSort: () -> Unit = {},
    // Card tap opens media detail, pencil opens the entry editor.
    onOpenDetail: (Int) -> Unit = {},
    onEditEntry: (Int) -> Unit = {},
) {
    MediaListScreenScaffold(
        holder = holder,
        selected = selected,
        onSelectSection = onSelectSection,
        onReload = onReload,
        allSection = AnimeListSection.ALL,
        statusSections = ::animeStatusSections,
        sectionLabel = { section, s -> section.label(s) },
        emptyIcon = AppIcons.Anime,
        meta = ::animeEntryMeta,
        formatOptions = { s -> mediaListFormatOptions(isAnimeTab = true, strings = s) },
        cardContent = { entry ->
            AnimeListEntryContent(
                entry = entry,
                onIncrement = { holder.incrementProgress(entry) },
                strings = strings,
                isSmileyFormat = settings.isSmileyFormat,
                formatSmiley = settings.formatSmiley,
            )
        },
        settings = settings,
        strings = strings,
        modifier = modifier,
        bottomPadding = bottomPadding,
        isRefreshing = isRefreshing,
        isFilterOpen = isFilterOpen,
        onDismissFilter = onDismissFilter,
        isSortOpen = isSortOpen,
        onDismissSort = onDismissSort,
        onOpenDetail = onOpenDetail,
        onEditEntry = onEditEntry,
    )
}

/** Anime meta line: format + season/year + work status. */
private fun animeEntryMeta(entry: AnimeListEntry, strings: LanguageStrings): String =
    catalogMetaLine(
        format = entry.format.label(strings.formatUnknownLabel),
        season = entry.season?.label(strings),
        year = entry.seasonYear,
        status = entry.mediaStatus?.label(strings, isManga = false),
    )
