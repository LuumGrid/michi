package com.luum.michi.app.mediaList.repository.anime

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.repository.fetchMediaListCollection

internal class AnimeListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AnimeListRepository {

    override suspend fun loadList(
        userId: Int,
        splitCompleted: Boolean,
    ): NetworkResult<List<AnimeListEntry>> =
        graphQLClient.fetchMediaListCollection(userId, MediaType.ANIME)
            .map { entries ->
                entries.mapIndexed { index, entry -> entry.toAnimeListEntry(index, splitCompleted) }
            }
}
