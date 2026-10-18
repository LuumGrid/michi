package com.luum.michi.app.mediaList.ui.manga.state

import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.medialist.domain.SortScope
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.incrementedVolumes
import com.luum.michi.app.mediaList.domain.manga.model.toMediaListStatus
import com.luum.michi.app.mediaList.ui.common.state.MediaListStateHolderBase
import kotlinx.coroutines.CoroutineScope

internal class MangaListStateHolder(
    private val repository: MangaListRepository,
    private val entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    sortPersistence: SortPersistence? = null,
    sortScope: SortScope = SortScope.MANGA,
    onEntrySaved: () -> Unit = {},
) : MediaListStateHolderBase<MangaListEntry, MangaListSection>(
    scope = scope,
    sortPersistence = sortPersistence,
    sortScope = sortScope,
    onEntrySaved = onEntrySaved,
) {
    override val allSection = MangaListSection.ALL

    override fun statusOf(entry: MangaListEntry): MangaListSection = entry.status

    override val progressOrder: Comparator<MangaListEntry> = compareBy { it.chaptersProgress }

    // AniList exposes no next-chapter date for manga: degrade to list order.
    override val airingOrder: Comparator<MangaListEntry> = compareBy { it.originalIndex }

    override suspend fun fetchList(userId: Int, splitCompleted: Boolean): NetworkResult<List<MangaListEntry>> =
        repository.loadList(userId, splitCompleted)

    override suspend fun loadCachedList(userId: Int, splitCompleted: Boolean): List<MangaListEntry>? =
        repository.loadCachedList(userId, splitCompleted)

    override suspend fun saveProgress(entry: MangaListEntry): NetworkResult<Unit> =
        entryRepository.saveProgress(
            mediaId = entry.id,
            // Novels track volumes only: chapters stay omitted so the
            // server leaves them intact instead of resetting to stale 0.
            progress = if (entry.tracksByVolume) null else entry.chaptersProgress,
            status = entry.status.toMediaListStatus(),
            progressVolumes = entry.volumesProgress,
        )

    fun incrementChapters(entry: MangaListEntry) {
        enqueue(entry.id) { it.incrementedChapters(lastSplitCompleted) }
    }

    fun incrementVolumes(entry: MangaListEntry) {
        enqueue(entry.id) { it.incrementedVolumes(lastSplitCompleted) }
    }
}
