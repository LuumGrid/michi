package com.luum.michi.app.mediaList.ui.anime

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.animeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.common.mediaListFormatOptions
import com.luum.michi.app.mediaList.ui.anime.components.AnimeListEntryContent
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.common.MediaListScreenScaffold
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

/** Anime meta line: format + season/year + work status. */
private fun animeEntryMeta(entry: AnimeListEntry, strings: LanguageStrings): String {
    val seasonYear = entry.season?.let { season ->
        val year = entry.seasonYear?.toString()
        if (year != null) "${season.label(strings)} $year" else season.label(strings)
    } ?: entry.seasonYear?.toString()
    return listOfNotNull(
        entry.format.label(strings.formatUnknownLabel),
        seasonYear,
        entry.mediaStatus?.label(strings, isManga = false),
    ).joinToString(" · ")
}
