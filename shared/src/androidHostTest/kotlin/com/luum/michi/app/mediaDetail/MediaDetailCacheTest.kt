package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.domain.language.EnglishStrings
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffPage
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

private fun detail(id: Int) = MediaDetail(
    id = id,
    type = MediaDetailType.ANIME,
    title = "Title",
    coverUrl = null,
    bannerUrl = null,
    paletteHex = null,
    format = null,
    status = null,
    episodes = null,
    chapters = null,
    volumes = null,
    duration = null,
    genres = emptyList(),
    studios = emptyList(),
    source = null,
    season = null,
    startedLabel = null,
    endedLabel = null,
    averageScore = null,
    meanScore = null,
    popularity = null,
    favourites = null,
    descriptionPlain = "",
    isAdult = false,
    isFavourite = false,
    viewerEntry = null,
    relations = emptyList(),
    scoreDistribution = emptyList(),
    statusDistribution = emptyList(),
    characters = MediaCharactersPage(emptyList(), false, 1),
    staff = MediaStaffPage(emptyList(), false, 1),
)

private class FakeDetailRepository : MediaDetailRepository {
    var calls = 0

    override suspend fun loadDetail(
        mediaId: Int,
        voiceLanguage: String,
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> {
        calls++
        return NetworkResult.Success(detail(mediaId))
    }

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<MediaCharactersPage> = throw NotImplementedError()

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage> = throw NotImplementedError()

    override suspend fun loadRecommendations(
        mediaId: Int,
    ): NetworkResult<List<MediaRecommendationEntry>> = throw NotImplementedError()
}

class MediaDetailCacheTest {

    @Test
    fun secondLoadForSameIdHitsCache() {
        val repository = FakeDetailRepository()
        val holder = MediaDetailStateHolder(repository, CoroutineScope(Dispatchers.Unconfined), EnglishStrings)
        holder.load(1)
        holder.load(1)
        assertEquals(1, repository.calls)
        assertEquals(1, holder.detail?.id)
    }

    @Test
    fun refreshBypassesCache() {
        val repository = FakeDetailRepository()
        val holder = MediaDetailStateHolder(repository, CoroutineScope(Dispatchers.Unconfined), EnglishStrings)
        holder.load(1)
        holder.refresh()
        assertEquals(2, repository.calls)
    }
}
