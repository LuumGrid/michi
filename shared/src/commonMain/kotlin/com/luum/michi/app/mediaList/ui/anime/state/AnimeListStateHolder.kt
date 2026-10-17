package com.luum.michi.app.mediaList.ui.anime.state

import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.medialist.domain.SortScope
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.mediaList.domain.anime.model.toMediaListStatus
import com.luum.michi.app.mediaList.ui.common.state.MediaListStateHolderBase
import kotlinx.coroutines.CoroutineScope

internal class AnimeListStateHolder(
    private val repository: AnimeListRepository,
    private val entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    sortPersistence: SortPersistence? = null,
    sortScope: SortScope = SortScope.ANIME,
    onEntrySaved: () -> Unit = {},
) : MediaListStateHolderBase<AnimeListEntry, AnimeListSection>(
    scope = scope,
    sortPersistence = sortPersistence,
    sortScope = sortScope,
    onEntrySaved = onEntrySaved,
) {
    override val allSection = AnimeListSection.ALL

    override fun statusOf(entry: AnimeListEntry): AnimeListSection = entry.status

    override val progressOrder: Comparator<AnimeListEntry> = compareBy { it.progress }

    override val airingOrder: Comparator<AnimeListEntry> = compareBy { it.nextAiringAt }

    override suspend fun fetchList(userId: Int, splitCompleted: Boolean): NetworkResult<List<AnimeListEntry>> =
        repository.loadList(userId, splitCompleted)

    override suspend fun saveProgress(entry: AnimeListEntry): NetworkResult<Unit> =
        entryRepository.saveProgress(
            mediaId = entry.id,
            progress = entry.progress,
            status = entry.status.toMediaListStatus(),
        )

    fun incrementProgress(entry: AnimeListEntry) {
        enqueue(entry.id) { it.incremented(lastSplitCompleted) }
    }
}
