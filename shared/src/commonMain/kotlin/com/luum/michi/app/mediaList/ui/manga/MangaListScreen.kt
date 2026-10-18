package com.luum.michi.app.mediaList.ui.manga

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.catalogMetaLine
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.mediaList.domain.common.mediaListFormatOptions
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.completesOnChapterIncrement
import com.luum.michi.app.mediaList.domain.manga.model.completesOnVolumeIncrement
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.mediaList.domain.manga.model.mangaStatusSections
import com.luum.michi.app.mediaList.ui.common.MediaListScreenScaffold
import com.luum.michi.app.mediaList.ui.common.MediaListScreenSettings
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
    // The `+1` that lands on the total opens the editor in Completed instead of
    // saving from the list (Otraku/AL-chan): cancelling leaves the entry as is.
    onCompleteEntry: (mediaId: Int, progress: Int) -> Unit,
) {
    MediaListScreenScaffold(
        holder = holder,
        selected = selected,
        onSelectSection = onSelectSection,
        onReload = onReload,
        allSection = MangaListSection.ALL,
        statusSections = ::mangaStatusSections,
        sectionLabel = { section, s -> section.label(s) },
        emptyIcon = AppIcons.Manga,
        meta = ::mangaEntryMeta,
        formatOptions = { s -> mediaListFormatOptions(isAnimeTab = false, strings = s) },
        cardContent = { entry ->
            MangaListEntryContent(
                entry = entry,
                onIncrementChapters = {
                    val total = entry.totalChapters
                    if (total != null && entry.completesOnChapterIncrement()) {
                        // The completing step is the editor's write, not a list save.
                        onCompleteEntry(entry.id, total)
                    } else {
                        holder.incrementChapters(entry)
                    }
                },
                onIncrementVolumes = {
                    if (entry.completesOnVolumeIncrement()) {
                        // The editor tracks chapters; Completed fills volumes
                        // (and chapters to the total when known).
                        onCompleteEntry(entry.id, entry.chaptersProgress)
                    } else {
                        holder.incrementVolumes(entry)
                    }
                },
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

/** Manga meta line: format + year + work status (no airing data for manga). */
private fun mangaEntryMeta(entry: MangaListEntry, strings: LanguageStrings): String =
    catalogMetaLine(
        format = entry.format.label(strings.formatUnknownLabel),
        season = null,
        year = entry.seasonYear,
        status = entry.mediaStatus?.label(strings, isManga = true),
    )
