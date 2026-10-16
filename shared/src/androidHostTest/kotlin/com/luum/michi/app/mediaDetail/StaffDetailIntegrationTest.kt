package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterPage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaPage
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun staffMediaItem(id: Int) = StaffMediaItem(
    mediaId = id,
    title = "Media $id",
    cover = null,
    paletteHex = null,
    staffRole = "Director",
    format = null,
    year = null,
    averageScore = null,
)

private fun staffCharacterItem(id: Int) = StaffCharacterItem(
    characterId = id,
    name = "Character $id",
    image = null,
    mediaTitle = "Media $id",
)

private fun staffDetail(
    id: Int,
    media: StaffMediaPage = StaffMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
    characters: StaffCharacterPage = StaffCharacterPage(emptyList(), hasNextPage = false, currentPage = 1),
    isFavourite: Boolean = false,
) = StaffDetail(
    id = id,
    name = "Staff $id",
    nativeName = null,
    alternativeNames = emptyList(),
    image = null,
    descriptionPlain = "",
    stats = emptyList(),
    links = emptyList(),
    gender = null,
    age = null,
    birthday = null,
    death = null,
    yearsActiveStart = null,
    yearsActiveEnd = null,
    homeTown = null,
    occupations = null,
    bloodType = null,
    favourites = null,
    isFavourite = isFavourite,
    media = media,
    characters = characters,
)

/**
 * Repository double: canned media/character pages plus call counters to prove
 * cache hits, sort reloads and per-list pagination.
 */
private class RecordingStaffRepository(
    private val mediaPages: Map<Int, StaffMediaPage> = emptyMap(),
    private val characterPages: Map<Int, StaffCharacterPage> = emptyMap(),
) : StaffDetailRepository {
    var detailCalls = 0
        private set
    var mediaCalls = 0
        private set
    var characterCalls = 0
        private set
    private var favourite = false

    override suspend fun loadDetail(
        id: Int,
        strings: LanguageStrings,
    ): NetworkResult<StaffDetail> {
        detailCalls++
        return NetworkResult.Success(
            staffDetail(
                id = id,
                media = mediaPages[1] ?: StaffMediaPage(emptyList(), false, 1),
                characters = characterPages[1] ?: StaffCharacterPage(emptyList(), false, 1),
                isFavourite = favourite,
            ),
        )
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
    ): NetworkResult<StaffMediaPage> {
        mediaCalls++
        return NetworkResult.Success(mediaPages[page] ?: StaffMediaPage(emptyList(), false, page))
    }

    override suspend fun loadCharacterPage(id: Int, page: Int): NetworkResult<StaffCharacterPage> {
        characterCalls++
        return NetworkResult.Success(characterPages[page] ?: StaffCharacterPage(emptyList(), false, page))
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        favourite = !favourite
        return NetworkResult.Success(Unit)
    }
}

private fun holderWired(repository: StaffDetailRepository) = StaffDetailStateHolder(
    repository = repository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    stringsProvider = { EnglishStrings },
)

class StaffDetailIntegrationTest {

    @Test
    fun loadHydratesDetailMediaAndCharacters() {
        val repository = RecordingStaffRepository(
            mediaPages = mapOf(1 to StaffMediaPage(listOf(staffMediaItem(7)), false, 1)),
            characterPages = mapOf(1 to StaffCharacterPage(listOf(staffCharacterItem(9)), false, 1)),
        )
        val holder = holderWired(repository)

        holder.load(3)

        assertEquals("Staff 3", holder.detail?.name)
        assertEquals(listOf(7), holder.mediaItems.map { it.mediaId })
        assertEquals(listOf(9), holder.characterItems.map { it.characterId })
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun cacheHitSkipsNetworkOnRevisit() {
        val repository = RecordingStaffRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        holder.load(1)

        assertEquals(2, repository.detailCalls)
        assertEquals(1, holder.detail?.id)
    }

    @Test
    fun loadMoreMediaAppendsNextPage() {
        val repository = RecordingStaffRepository(
            mediaPages = mapOf(
                1 to StaffMediaPage(listOf(staffMediaItem(1)), hasNextPage = true, currentPage = 1),
                2 to StaffMediaPage(listOf(staffMediaItem(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertTrue(holder.mediaHasNextPage)

        holder.loadMoreMedia()

        assertEquals(listOf(1, 2), holder.mediaItems.map { it.mediaId })
        assertFalse(holder.mediaHasNextPage)
        assertEquals(1, repository.mediaCalls)
    }

    @Test
    fun toggleFavouriteFlips() {
        val repository = RecordingStaffRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)

        holder.toggleFavourite()
        assertTrue(holder.isFavourite)

        holder.toggleFavourite()
        assertFalse(holder.isFavourite)
    }
}

private class GatedStaffRepository : StaffDetailRepository by RecordingStaffRepository() {
    val gate = CompletableDeferred<Unit>()

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        gate.await()
        return NetworkResult.Success(Unit)
    }
}

/** See `MediaDetailFavouriteCacheTest`: the late response must not mix entries. */
class StaffDetailFavouriteCacheTest {

    @Test
    fun togglingOnAnotherStaffKeepsEachCacheEntrySeparate() {
        val repository = GatedStaffRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)
        holder.toggleFavourite()
        assertTrue(holder.isFavourite, "optimistic")

        holder.load(2)
        assertFalse(holder.isFavourite, "the visible staff member is the second one")

        repository.gate.complete(Unit)

        assertFalse(holder.isFavourite, "the late response does not touch the visible one")
        holder.load(1)
        assertTrue(holder.isFavourite, "the first staff member's snapshot kept the toggle")
    }

    @Test
    fun twoRolesOnTheSameWorkAreBothKept() {
        val roles = StaffMediaPage(
            listOf(
                staffMediaItem(7).copy(staffRole = "Director"),
                staffMediaItem(7).copy(staffRole = "Storyboard"),
            ),
            hasNextPage = false,
            currentPage = 1,
        )
        val holder = holderWired(RecordingStaffRepository(mediaPages = mapOf(1 to roles)))

        holder.load(1)

        assertEquals(
            listOf("Director", "Storyboard"),
            holder.mediaItems.map { it.staffRole },
            "dedup is by work + role, not by work alone",
        )
    }
}
