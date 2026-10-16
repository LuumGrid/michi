package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.repository.fetchMediaListCollection

internal class MangaListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : MangaListRepository {

    override suspend fun loadList(
        userId: Int,
        splitCompleted: Boolean,
    ): NetworkResult<List<MangaListEntry>> =
        graphQLClient.fetchMediaListCollection(userId, MediaType.MANGA)
            .map { entries ->
                entries.mapIndexed { index, entry -> entry.toMangaListEntry(index, splitCompleted) }
            }
}
