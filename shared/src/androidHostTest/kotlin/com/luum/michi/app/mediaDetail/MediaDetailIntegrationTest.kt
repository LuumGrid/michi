package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterRole
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaEntryEditorData
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaTrailer
import com.luum.michi.app.mediaDetail.domain.media.model.StudioRef
import com.luum.michi.app.mediaDetail.domain.media.model.aniListUrl
import com.luum.michi.app.mediaDetail.domain.media.model.shareText
import com.luum.michi.app.mediaDetail.ui.media.components.hasInformation
import com.luum.michi.app.mediaDetail.ui.media.components.trailerUrl
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun sampleDetail(
    id: Int,
    characters: Page<MediaCharacterEntry> = Page(emptyList(), hasNextPage = false, currentPage = 1),
    staff: Page<MediaStaffEntry> = Page(emptyList(), hasNextPage = false, currentPage = 1),
    recommendations: Page<MediaRecommendationEntry> = Page(emptyList(), hasNextPage = false, currentPage = 1),
    viewerEntry: MediaListViewerEntry? = null,
    isFavourite: Boolean = false,
) = MediaDetail(
    id = id,
    type = MediaDetailType.ANIME,
    title = "Media $id",
    cover = null,
    bannerUrl = null,
    paletteHex = null,
    format = "TV",
    status = MediaWorkStatus.RELEASING,
    episodes = 12,
    chapters = null,
    volumes = null,
    duration = null,
    genres = emptyList(),
    studios = emptyList(),
    producers = emptyList(),
    source = null,
    season = null,
    startedLabel = null,
    endedLabel = null,
    averageScore = null,
    popularity = null,
    favourites = null,
    descriptionPlain = "",
    isAdult = false,
    isFavourite = isFavourite,
    viewerEntry = viewerEntry,
    relations = emptyList(),
    characters = characters,
    staff = staff,
    recommendations = recommendations,
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
)

/**
 * Repository double: canned pages per media id, call counters to prove cache
 * hits and voice-language reloads, and an optional failure switch.
 */
private class RecordingDetailRepository(
    private val charactersPages: Map<Int, Page<MediaCharacterEntry>> = emptyMap(),
    private val staffPages: Map<Int, Page<MediaStaffEntry>> = emptyMap(),
    private val recommendationsPages: Map<Int, Page<MediaRecommendationEntry>> = emptyMap(),
    private val failDetail: Boolean = false,
    private val viewerEntry: MediaListViewerEntry? = null,
    private val isFavourite: Boolean = false,
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
                characters = charactersPages[1] ?: Page(emptyList(), false, 1),
                staff = staffPages[1] ?: Page(emptyList(), false, 1),
                // The first recommendations page now travels inside the detail.
                recommendations = recommendationsPages[1] ?: Page(emptyList(), false, 1),
                viewerEntry = viewerEntry,
                isFavourite = isFavourite,
            ),
        )
    }

    override suspend fun loadEditorData(mediaId: Int): NetworkResult<MediaEntryEditorData> =
        NetworkResult.Success(
            MediaEntryEditorData(
                title = "Media $mediaId",
                type = MediaDetailType.ANIME,
                episodes = null,
                chapters = null,
                volumes = null,
                isFavourite = false,
                viewerEntry = viewerEntry,
            ),
        )

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaCharacterEntry>> {
        characterCalls++
        return NetworkResult.Success(charactersPages[page] ?: Page(emptyList(), false, page))
    }

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaStaffEntry>> {
        staffCalls++
        return NetworkResult.Success(staffPages[page] ?: Page(emptyList(), false, page))
    }

    override suspend fun loadRecommendations(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaRecommendationEntry>> {
        recommendationCalls++
        return NetworkResult.Success(
            recommendationsPages[page] ?: Page(emptyList(), false, page),
        )
    }
}

/** Holds the characters page 2 at [gate], so a page in flight can be interrupted. */
private class GatedPageDetailRepository(
    charactersPages: Map<Int, Page<MediaCharacterEntry>> = emptyMap(),
) : MediaDetailRepository by RecordingDetailRepository(charactersPages = charactersPages) {
    val gate = CompletableDeferred<Unit>()

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaCharacterEntry>> {
        if (page > 1) gate.await()
        return NetworkResult.Success(
            Page(listOf(character(mediaId * 100 + page)), hasNextPage = false, currentPage = page),
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

private class GatedEntryRepository(
    private val fail: Boolean = false,
) : MediaListEntryRepository by NoopEntryRepository {
    val gate = CompletableDeferred<Unit>()

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> {
        gate.await()
        return if (fail) {
            NetworkResult.Failure(NetworkError.Http(500, null))
        } else {
            NetworkResult.Success(Unit)
        }
    }
}

private fun holderWired(
    repository: MediaDetailRepository,
    entryRepository: MediaListEntryRepository = NoopEntryRepository,
) = MediaDetailStateHolder(
    repository = repository,
    entryRepository = entryRepository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    stringsProvider = { EnglishStrings },
)

class MediaDetailIntegrationTest {

    @Test
    fun loadHydratesDetailCharactersStaffAndRecommendations() {
        val characters = Page(listOf(character(1), character(2)), false, 1)
        val staff = Page(listOf(staffEntry(9)), false, 1)
        val recommendations = Page(listOf(recommendation(4)), false, 1)
        val repository = RecordingDetailRepository(
            charactersPages = mapOf(1 to characters),
            staffPages = mapOf(1 to staff),
            recommendationsPages = mapOf(1 to recommendations),
        )
        val holder = holderWired(repository)

        holder.load(5)

        assertEquals("Media 5", holder.detail?.title)
        assertEquals(2, holder.characters.items.size)
        assertEquals(1, holder.staff.items.size)
        assertEquals(listOf(4), holder.recommendations.items.map { it.id })
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
        // Recommendations ride the detail query: no extra page-1 call.
        assertEquals(0, repository.recommendationCalls)
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

    /**
     * The favourite router (a removal made elsewhere) mirrors into this holder:
     * [DetailStateHolderBase.applyFavourite] flips the visible page and the cached
     * snapshot, and [DetailStateHolderBase.favouriteFor] reads both back.
     */
    @Test
    fun applyFavouriteMirrorsTheVisiblePageAndTheCachedSnapshot() {
        val repository = RecordingDetailRepository(isFavourite = true)
        val holder = holderWired(repository)

        holder.load(1)
        assertTrue(holder.isFavourite)
        assertEquals(true, holder.favouriteFor(1))
        assertNull(holder.favouriteFor(2), "an unknown id has no favourite state")

        // Navigate away: media 1 lives in the LRU cache now.
        holder.load(2)
        assertEquals(2, holder.detail?.id)

        holder.applyFavourite(1, false)
        assertEquals(false, holder.favouriteFor(1), "the cached snapshot flipped")
        assertTrue(holder.isFavourite, "media 2 (on screen) is untouched")
        assertEquals(true, holder.favouriteFor(2))
    }

    @Test
    fun loadMoreCharactersAppendsNextPage() {
        val repository = RecordingDetailRepository(
            charactersPages = mapOf(
                1 to Page(listOf(character(1)), hasNextPage = true, currentPage = 1),
                2 to Page(listOf(character(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertEquals(1, holder.characters.items.size)
        assertTrue(holder.characters.hasNextPage)

        holder.loadMoreCharacters()

        assertEquals(listOf(1, 2), holder.characters.items.map { it.characterId })
        assertFalse(holder.characters.hasNextPage)
        assertEquals(1, repository.characterCalls)
    }

    @Test
    fun aCharactersPageInFlightIsDroppedWhenAnotherMediaLoads() {
        val repository = GatedPageDetailRepository(
            charactersPages = mapOf(1 to Page(emptyList(), hasNextPage = true, currentPage = 1)),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertTrue(holder.characters.hasNextPage)
        holder.loadMoreCharacters() // page 2, held at the gate

        holder.load(2) // another media cancels it
        repository.gate.complete(Unit)

        assertTrue(holder.characters.items.isEmpty(), "the stale page never appends")
        assertFalse(holder.characters.isLoadingMore, "its spinner is dropped")
    }

    @Test
    fun loadMoreRecommendationsAppendsNextPage() {
        val repository = RecordingDetailRepository(
            recommendationsPages = mapOf(
                1 to Page(listOf(recommendation(1)), hasNextPage = true, currentPage = 1),
                2 to Page(listOf(recommendation(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)

        // Page 1 arrived inside the detail response.
        assertEquals(listOf(1), holder.recommendations.items.map { it.id })
        assertTrue(holder.recommendations.hasNextPage)

        holder.loadMoreRecommendations()

        assertEquals(listOf(1, 2), holder.recommendations.items.map { it.id })
        assertFalse(holder.recommendations.hasNextPage)
        // Only page 2 went to the network.
        assertEquals(1, repository.recommendationCalls)
    }

    @Test
    fun cacheRestoreKeepsTheCachedMediasRecommendationsWithoutNetwork() {
        val repository = RecordingDetailRepository(
            recommendationsPages = mapOf(
                1 to Page(listOf(recommendation(1)), hasNextPage = false, currentPage = 1),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        // Back to 1: the snapshot (recommendations included) restores with no call.
        holder.load(1)

        assertEquals(1, holder.detail?.id)
        assertEquals(listOf(1), holder.recommendations.items.map { it.id })
        assertEquals(2, repository.detailCalls)
        assertEquals(0, repository.recommendationCalls)
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

/**
 * Media's favourite is optimistic like the other three scopes: the heart flips
 * before the repository answers, a failure reverts the visible page and its
 * cached snapshot, and a late failure never touches a media opened meanwhile.
 */
class MediaDetailOptimisticFavouriteTest {

    @Test
    fun theHeartFlipsBeforeTheRepositoryAnswers() {
        val entryRepository = GatedEntryRepository()
        val holder = holderWired(RecordingDetailRepository(), entryRepository)

        holder.load(1)
        assertFalse(holder.detail?.isFavourite == true)

        holder.toggleFavourite()

        assertTrue(holder.detail?.isFavourite == true, "optimistic, before the gate")
        assertTrue(holder.isTogglingFavourite)

        entryRepository.gate.complete(Unit)

        assertTrue(holder.detail?.isFavourite == true, "the success keeps the new value")
        assertFalse(holder.isTogglingFavourite)
    }

    @Test
    fun aFailedToggleRevertsTheDetailAndTheCache() {
        val entryRepository = GatedEntryRepository(fail = true)
        val holder = holderWired(RecordingDetailRepository(), entryRepository)

        holder.load(1)
        holder.toggleFavourite()
        assertTrue(holder.detail?.isFavourite == true, "optimistic")

        entryRepository.gate.complete(Unit)

        assertFalse(holder.detail?.isFavourite == true, "the failure reverts the visible page")
        assertIs<NetworkError.Http>(holder.actionError)

        holder.load(2)
        holder.load(1)
        assertFalse(holder.detail?.isFavourite == true, "the cached snapshot is reverted too")
    }

    @Test
    fun aFailedToggleAfterOpeningAnotherMediaLeavesTheVisibleOneAlone() {
        val entryRepository = GatedEntryRepository(fail = true)
        val holder = holderWired(RecordingDetailRepository(), entryRepository)

        holder.load(1)
        holder.toggleFavourite() // A optimistic true, held at the gate
        holder.load(2) // the user opens B meanwhile

        entryRepository.gate.complete(Unit) // A's failure lands

        assertFalse(holder.detail?.isFavourite == true, "B is untouched")
        assertNull(holder.actionError, "the failure belongs to A, not the visible B")

        holder.load(1)
        assertFalse(holder.detail?.isFavourite == true, "A reverted in its own cache")
    }
}

/** The pure gates the screen uses to emit the information and videos items. */
class MediaDetailInformationTest {

    private fun withoutInformation(detail: MediaDetail) = detail.copy(
        format = null,
        status = null,
        episodes = null,
        chapters = null,
        volumes = null,
        duration = null,
        season = null,
        startedLabel = null,
        endedLabel = null,
        source = null,
        studios = emptyList(),
        producers = emptyList(),
        titleRomaji = null,
        titleNative = null,
        synonyms = emptyList(),
    )

    @Test
    fun hasInformationIsFalseOnlyWhenNothingWouldPaint() {
        val empty = withoutInformation(sampleDetail(1))

        assertFalse(empty.hasInformation(isManga = false))
        assertFalse(empty.hasInformation(isManga = true))
    }

    @Test
    fun hasInformationIsTrueForAnyPaintableGroup() {
        // An anime identity field.
        assertTrue(sampleDetail(1).hasInformation(isManga = false))
        // A manga-only count.
        assertTrue(
            sampleDetail(1).copy(episodes = null, format = null, status = null, chapters = 5)
                .hasInformation(isManga = true),
        )
        // A wide title.
        assertTrue(
            withoutInformation(sampleDetail(1)).copy(titleNative = "日本語")
                .hasInformation(isManga = false),
        )
        // A studio chip.
        assertTrue(
            withoutInformation(sampleDetail(1)).copy(studios = listOf(StudioRef(1, "Bones")))
                .hasInformation(isManga = false),
        )
    }

    @Test
    fun trailerUrlIsNullForMissingOrUnresolvableTrailers() {
        assertNull(sampleDetail(1).trailerUrl())
        assertNull(sampleDetail(1).copy(trailer = MediaTrailer("abc", "vimeo", null)).trailerUrl())
        assertEquals(
            "https://youtu.be/abc",
            sampleDetail(1).copy(trailer = MediaTrailer("abc", "youtube", null)).trailerUrl(),
        )
    }
}
