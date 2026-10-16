package com.luum.michi.app.mediaList.ui.manga

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.mediaList.domain.common.mediaListFormatOptions
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.mediaList.domain.manga.model.mangaStatusSections
import com.luum.michi.app.mediaList.ui.common.MediaListScreenScaffold
import com.luum.michi.app.mediaList.ui.manga.components.MangaListEntryContent
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Manga list: thin wrapper over [MediaListScreenScaffold] that fills only what
 * differs — sections, empty icon, meta line, format options and the entry body
 * (chapters, volumes for novels). Manga exposes no airing dates, so the meta
 * line is format + year.
 */
@Composable
internal fun MangaListScreen(
    holder: MangaListStateHolder,
    selected: MangaListSection,
    onSelectSection: (MangaListSection) -> Unit,
    onRetry: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last item clears the floating
    // TabBar with the same 16dp rhythm the first item keeps under the rail.
    bottomPadding: Dp = 0.dp,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    // In-context search: filters the current section by title (bestTitle
    // only — v1 limitation). Empty query shows the section untouched.
    query: String = "",
    // List tools (toolbar filter/sort): Root owns the open flags so the
    // toolbar back affordance and system back close the sheets.
    isFilterOpen: Boolean = false,
    onDismissFilter: () -> Unit = {},
    isSortOpen: Boolean = false,
    onDismissSort: () -> Unit = {},
    // Mirrors the Settings persist toggle: when on, sort changes are
    // remembered per tab across restarts (filters never persist).
    sortPersist: Boolean = false,
    // Mirrors the Settings split toggle: merged COMPLETED when off,
    // per-format sections when on.
    splitCompleted: Boolean = true,
    // Global 18+ setting (inverted): hidden entries never reach the
    // list, but rail counts stay raw (see holder.countInSection).
    hideAdult: Boolean = false,
    // Smiley score format: the card pill shows the glyph instead of
    // star + number (translated in Root; settings never crosses here).
    isSmileyFormat: Boolean = false,
    formatSmiley: (Float) -> String = { "" },
    // Card tap opens media detail, pencil opens the entry editor (both
    // dormant until their surfaces land; intents already exist in State).
    onOpenDetail: (Int) -> Unit = {},
    onEditEntry: (Int) -> Unit = {},
) {
    MediaListScreenScaffold(
        holder = holder,
        selected = selected,
        onSelectSection = onSelectSection,
        onRetry = onRetry,
        allSection = MangaListSection.ALL,
        statusSections = ::mangaStatusSections,
        sectionLabel = { section, s -> section.label(s) },
        emptyIcon = AppIcons.Manga,
        meta = ::mangaEntryMeta,
        formatOptions = { s -> mediaListFormatOptions(isAnimeTab = false, strings = s) },
        cardContent = { entry ->
            MangaListEntryContent(
                entry = entry,
                onIncrementChapters = { holder.incrementChapters(entry) },
                onIncrementVolumes = { holder.incrementVolumes(entry) },
                strings = strings,
                isSmileyFormat = isSmileyFormat,
                formatSmiley = formatSmiley,
            )
        },
        strings = strings,
        modifier = modifier,
        bottomPadding = bottomPadding,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        query = query,
        isFilterOpen = isFilterOpen,
        onDismissFilter = onDismissFilter,
        isSortOpen = isSortOpen,
        onDismissSort = onDismissSort,
        sortPersist = sortPersist,
        splitCompleted = splitCompleted,
        hideAdult = hideAdult,
        onOpenDetail = onOpenDetail,
        onEditEntry = onEditEntry,
    )
}

/** Manga meta line: format + year + work status (no airing data for manga). */
private fun mangaEntryMeta(entry: MangaListEntry, strings: LanguageStrings): String =
    listOfNotNull(
        entry.format.label(strings.formatUnknownLabel),
        entry.seasonYear?.toString(),
        entry.mediaStatus?.label(strings, isManga = true),
    ).joinToString(" · ")
