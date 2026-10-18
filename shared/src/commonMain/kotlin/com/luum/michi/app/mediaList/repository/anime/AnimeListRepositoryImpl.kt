package com.luum.michi.app.mediaList.repository.anime

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.repository.MediaListDiskCache
import com.luum.michi.app.mediaList.repository.fetchMediaListCollection

internal class AnimeListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val cache: MediaListDiskCache? = null,
) : AnimeListRepository {

    override suspend fun loadList(
        userId: Int,
        splitCompleted: Boolean,
    ): NetworkResult<List<AnimeListEntry>> {
        val result = graphQLClient.fetchMediaListCollection(userId, MediaType.ANIME)
        if (result is NetworkResult.Success) {
            cache?.write(userId, MediaType.ANIME, result.value)
        }
        return result.map { entries ->
            entries.mapIndexed { index, entry -> entry.toAnimeListEntry(index, splitCompleted) }
        }
    }

    override suspend fun loadCachedList(userId: Int, splitCompleted: Boolean): List<AnimeListEntry>? =
        cache?.read(userId, MediaType.ANIME)
            ?.mapIndexed { index, entry -> entry.toAnimeListEntry(index, splitCompleted) }
}
