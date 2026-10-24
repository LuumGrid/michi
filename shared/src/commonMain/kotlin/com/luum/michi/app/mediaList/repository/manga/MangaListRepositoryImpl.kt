package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.repository.MediaListDiskCache
import com.luum.michi.app.mediaList.repository.fetchMediaListCollection

internal class MangaListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val cache: MediaListDiskCache? = null,
) : MangaListRepository {

    override suspend fun loadList(
        userId: Int,
        splitCompleted: Boolean,
    ): NetworkResult<List<MangaListEntry>> {
        val result = graphQLClient.fetchMediaListCollection(userId, MediaType.MANGA)
        if (result is NetworkResult.Success) {
            cache?.write(userId, MediaType.MANGA, result.value)
        }
        return result.map { entries ->
            entries.mapIndexed { index, entry -> entry.toMangaListEntry(index, splitCompleted) }
        }
    }

    override suspend fun loadCachedList(userId: Int, splitCompleted: Boolean): List<MangaListEntry>? =
        cache?.read(userId, MediaType.MANGA)
            ?.mapIndexed { index, entry -> entry.toMangaListEntry(index, splitCompleted) }
}
