package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationsPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffPage
import com.luum.michi.app.mediaDetail.domain.media.model.aniListUrl
import com.luum.michi.app.mediaDetail.domain.media.model.shareText
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun sampleDetail(
    id: Int,
    characters: MediaCharactersPage = MediaCharactersPage(emptyList(), hasNextPage = false, currentPage = 1),
    staff: MediaStaffPage = MediaStaffPage(emptyList(), hasNextPage = false, currentPage = 1),
    viewerEntry: MediaListViewerEntry? = null,
) = MediaDetail(
    id = id,
    type = MediaDetailType.ANIME,
    title = "Media $id",
    cover = null,
    bannerUrl = null,
    paletteHex = null,
    format = "TV",
    status = "Releasing",
    episodes = 12,
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
    viewerEntry = viewerEntry,
    relations = emptyList(),
    characters = characters,
    staff = staff,
    titleRomaji = null,
    titleNative = null,
    synonyms = emptyList(),
    tags = emptyList(),
    externalLinks = emptyList(),
    trailer = null,
)

private fun character(id: Int) = MediaCharacterEntry(
    edgeKey = "edge-$id",
    characterId = id,
    name = "Character $id",
    image = null,
    role = MediaCharacterRole.MAIN,
)

private fun staffEntry(id: Int) = MediaStaffEntry(
    edgeKey = "staff-$id",
    staffId = id,
    name = "Staff $id",
    image = null,
    role = "Director",
)

private fun recommendation(id: Int) = MediaRecommendationEntry(
    id = id,
    title = "Recommendation $id",
    cover = null,
    format = "TV",
    year = 2026,
    episodesCount = 12,
    chaptersCount = null,
    volumesCount = null,
    averageScore = null,
    favouritesCount = null,
    likesCount = 0,
    viewerStatus = null,
)

/**
 * Repository double: canned pages per media id, call counters to prove cache
 * hits and voice-language reloads, and an optional failure switch.
 */
private class RecordingDetailRepository(
    private val charactersPages: Map<Int, MediaCharactersPage> = emptyMap(),
    private val staffPages: Map<Int, MediaStaffPage> = emptyMap(),
    private val recommendationsPages: Map<Int, MediaRecommendationsPage> = emptyMap(),
    private val failDetail: Boolean = false,
    private val viewerEntry: MediaListViewerEntry? = null,
) : MediaDetailRepository {
    var detailCalls = 0
        private set
    var characterCalls = 0
        private set
    var staffCalls = 0
        private set
    var recommendationCalls = 0
        private set

    override suspend fun loadDetail(
        mediaId: Int,
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> {
        detailCalls++
        if (failDetail) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(
            sampleDetail(
                id = mediaId,
                characters = charactersPages[1] ?: MediaCharactersPage(emptyList(), false, 1),
                staff = staffPages[1] ?: MediaStaffPage(emptyList(), false, 1),
                viewerEntry = viewerEntry,
            ),
        )
    }

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaCharactersPage> {
        characterCalls++
        return NetworkResult.Success(charactersPages[page] ?: MediaCharactersPage(emptyList(), false, page))
    }

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage> {
        staffCalls++
        return NetworkResult.Success(staffPages[page] ?: MediaStaffPage(emptyList(), false, page))
    }

    override suspend fun loadRecommendations(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaRecommendationsPage> {
        recommendationCalls++
        return NetworkResult.Success(
            recommendationsPages[page] ?: MediaRecommendationsPage(emptyList(), false, page),
        )
    }
}

private object NoopEntryRepository : MediaListEntryRepository {
    override suspend fun saveEntry(
        mediaId: Int,
        status: MediaListStatus,
        progress: Int,
        progressVolumes: Int?,
        score: Float,
        advancedScores: List<Float>?,
        notes: String,
        repeat: Int,
        priority: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
        startedAtMillis: Long?,
        completedAtMillis: Long?,
    ): NetworkResult<MediaListViewerEntry> = NetworkResult.Failure(NetworkError.Unknown(null))

    override suspend fun saveProgress(
        mediaId: Int,
        progress: Int?,
        status: MediaListStatus?,
        progressVolumes: Int?,
    ): NetworkResult<Unit> = NetworkResult.Failure(NetworkError.Unknown(null))

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> =
        NetworkResult.Failure(NetworkError.Unknown(null))

    override suspend fun deleteEntry(entryId: Int): NetworkResult<Unit> =
        NetworkResult.Failure(NetworkError.Unknown(null))
}

private fun viewerEntry(progress: Int, status: MediaListStatus? = MediaListStatus.CURRENT) = MediaListViewerEntry(
    id = 7,
    status = status,
    progress = progress,
    progressVolumes = null,
    score = 0f,
    notes = "",
    repeat = 0,
    priority = 0,
    isPrivate = false,
    hiddenFromStatusLists = false,
    startedAtMillis = null,
    completedAtMillis = null,
)

private class GatedEntryRepository : MediaListEntryRepository by NoopEntryRepository {
    val gate = CompletableDeferred<Unit>()

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> {
        gate.await()
        return NetworkResult.Success(Unit)
    }
}

private fun holderWired(
    repository: MediaDetailRepository,
    entryRepository: MediaListEntryRepository = NoopEntryRepository,
) = MediaDetailStateHolder(
    repository = repository,
    entryRepository = entryRepository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    strings = EnglishStrings,
)

class MediaDetailIntegrationTest {

    @Test
    fun loadHydratesDetailCharactersAndStaff() {
        val characters = MediaCharactersPage(listOf(character(1), character(2)), false, 1)
        val staff = MediaStaffPage(listOf(staffEntry(9)), false, 1)
        val repository = RecordingDetailRepository(
            charactersPages = mapOf(1 to characters),
            staffPages = mapOf(1 to staff),
        )
        val holder = holderWired(repository)

        holder.load(5)

        assertEquals("Media 5", holder.detail?.title)
        assertEquals(2, holder.characters.size)
        assertEquals(1, holder.staff.size)
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun cacheHitSkipsNetworkOnRevisit() {
        val repository = RecordingDetailRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        holder.load(1)

        // Three loads, two fetches: the third restored from the LRU cache.
        assertEquals(2, repository.detailCalls)
        assertEquals(1, holder.detail?.id)
    }

    @Test
    fun loadMoreCharactersAppendsNextPage() {
        val repository = RecordingDetailRepository(
            charactersPages = mapOf(
                1 to MediaCharactersPage(listOf(character(1)), hasNextPage = true, currentPage = 1),
                2 to MediaCharactersPage(listOf(character(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertEquals(1, holder.characters.size)
        assertTrue(holder.charactersHasNextPage)

        holder.loadMoreCharacters()

        assertEquals(listOf(1, 2), holder.characters.map { it.characterId })
        assertFalse(holder.charactersHasNextPage)
        assertEquals(1, repository.characterCalls)
    }

    @Test
    fun loadMoreRecommendationsAppendsNextPage() {
        val repository = RecordingDetailRepository(
            recommendationsPages = mapOf(
                1 to MediaRecommendationsPage(listOf(recommendation(1)), hasNextPage = true, currentPage = 1),
                2 to MediaRecommendationsPage(listOf(recommendation(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        holder.loadRecommendations()

        assertEquals(listOf(1), holder.recommendations.map { it.id })
        assertTrue(holder.recommendationsHasNextPage)

        holder.loadMoreRecommendations()

        assertEquals(listOf(1, 2), holder.recommendations.map { it.id })
        assertFalse(holder.recommendationsHasNextPage)
        assertEquals(2, repository.recommendationCalls)
    }

    @Test
    fun cacheRestoreDropsThePreviousMediasRecommendations() {
        // The "View all" list re-binds the parent holder on back and refetches
        // page 1; a cached restore must not keep another media's recommendations.
        val repository = RecordingDetailRepository(
            recommendationsPages = mapOf(
                1 to MediaRecommendationsPage(listOf(recommendation(1)), hasNextPage = false, currentPage = 1),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        holder.loadRecommendations()
        holder.load(2)
        holder.load(1)

        assertEquals(1, holder.detail?.id)
        assertTrue(holder.recommendations.isEmpty())
    }

    @Test
    fun aniListUrlUsesTypeSegment() {
        assertEquals("https://anilist.co/anime/5", sampleDetail(5).aniListUrl())
        assertEquals(
            "https://anilist.co/manga/9",
            sampleDetail(9).copy(type = MediaDetailType.MANGA).aniListUrl(),
        )
    }

    @Test
    fun shareTextCombinesTitleAndUrl() {
        assertEquals("Media 5\nhttps://anilist.co/anime/5", sampleDetail(5).shareText())
    }

    @Test
    fun failureSurfacesErrorAndClearsLoading() {
        val repository = RecordingDetailRepository(failDetail = true)
        val holder = holderWired(repository)

        holder.load(1)

        assertNull(holder.detail)
        assertNotNull(holder.error)
        assertFalse(holder.isLoading)
    }

    @Test
    fun entryChangedDropsCachedSnapshot() {
        val repository = RecordingDetailRepository(viewerEntry = viewerEntry(5))
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        // Editor saved media 1 while 2 is showing: 1 must refetch next visit.
        holder.onEntryChanged(1)
        holder.load(1)

        assertEquals(3, repository.detailCalls)
    }
}

/**
 * The toggle response may land after the user opened another media: the cached
 * snapshot of the id that was toggled must keep its own value, and the visible
 * page must not change.
 */
class MediaDetailFavouriteCacheTest {

    @Test
    fun togglingOnAnotherMediaKeepsEachCacheEntrySeparate() {
        val entryRepository = GatedEntryRepository()
        val holder = holderWired(RecordingDetailRepository(), entryRepository)

        holder.load(1)
        assertFalse(holder.detail?.isFavourite == true)
        holder.toggleFavourite()
        holder.load(2)

        entryRepository.gate.complete(Unit)

        assertFalse(holder.detail?.isFavourite == true, "the visible media is untouched")
        holder.load(1)
        assertTrue(holder.detail?.isFavourite == true, "the first media's snapshot kept the toggle")
    }
}
