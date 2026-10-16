package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaPage
import com.luum.michi.app.mediaDetail.ui.character.state.CharacterDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun characterMediaItem(id: Int) = CharacterMediaItem(
    mediaId = id,
    title = "Media $id",
    cover = null,
    paletteHex = null,
    role = null,
    format = null,
    year = null,
    averageScore = null,
    viewerStatus = null,
    voiceActorName = null,
    voiceActorImage = null,
    voiceActorLanguage = null,
    voiceActorId = null,
)

private fun characterDetail(
    id: Int,
    media: CharacterMediaPage = CharacterMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
    isFavourite: Boolean = false,
) = CharacterDetail(
    id = id,
    name = "Character $id",
    nativeName = null,
    alternativeNames = emptyList(),
    image = null,
    descriptionPlain = "",
    stats = emptyList(),
    links = emptyList(),
    gender = null,
    age = null,
    bloodType = null,
    birthday = null,
    favourites = null,
    isFavourite = isFavourite,
    media = media,
)

/**
 * Repository double: canned media pages per page number plus call counters
 * to prove cache hits and sort reloads.
 */
private class RecordingCharacterRepository(
    private val mediaPages: Map<Int, CharacterMediaPage> = emptyMap(),
) : CharacterDetailRepository {
    var detailCalls = 0
        private set
    var mediaCalls = 0
        private set
    var toggleCalls = 0
        private set
    private var favourite = false

    override suspend fun loadDetail(
        id: Int,
        voiceLanguage: String,
        strings: LanguageStrings,
    ): NetworkResult<CharacterDetail> {
        detailCalls++
        return NetworkResult.Success(
            characterDetail(
                id = id,
                media = mediaPages[1] ?: CharacterMediaPage(emptyList(), false, 1),
                isFavourite = favourite,
            ),
        )
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<CharacterMediaPage> {
        mediaCalls++
        return NetworkResult.Success(mediaPages[page] ?: CharacterMediaPage(emptyList(), false, page))
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        toggleCalls++
        favourite = !favourite
        return NetworkResult.Success(Unit)
    }
}

private fun holderWired(repository: CharacterDetailRepository) = CharacterDetailStateHolder(
    repository = repository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    strings = EnglishStrings,
    viewerId = 0,
)

class CharacterDetailIntegrationTest {

    @Test
    fun loadHydratesDetailAndMedia() {
        val repository = RecordingCharacterRepository(
            mediaPages = mapOf(1 to CharacterMediaPage(listOf(characterMediaItem(7)), false, 1)),
        )
        val holder = holderWired(repository)

        holder.load(3)

        assertEquals("Character 3", holder.detail?.name)
        assertEquals(listOf(7), holder.mediaItems.map { it.mediaId })
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun cacheHitSkipsNetworkOnRevisit() {
        val repository = RecordingCharacterRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        holder.load(1)

        assertEquals(2, repository.detailCalls)
        assertEquals(1, holder.detail?.id)
    }

    @Test
    fun loadMoreAppendsNextPage() {
        val repository = RecordingCharacterRepository(
            mediaPages = mapOf(
                1 to CharacterMediaPage(listOf(characterMediaItem(1)), hasNextPage = true, currentPage = 1),
                2 to CharacterMediaPage(listOf(characterMediaItem(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertTrue(holder.hasNextPage)

        holder.loadMore()

        assertEquals(listOf(1, 2), holder.mediaItems.map { it.mediaId })
        assertFalse(holder.hasNextPage)
        assertEquals(1, repository.mediaCalls)
    }

    @Test
    fun toggleFavouriteFlips() {
        val repository = RecordingCharacterRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)

        holder.toggleFavourite()
        assertTrue(holder.isFavourite)

        holder.toggleFavourite()
        assertFalse(holder.isFavourite)
        assertEquals(2, repository.toggleCalls)
    }

    @Test
    fun selectVoiceLanguageRefetchesWithTheNewCast() {
        val repository = RecordingCharacterRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertEquals("JAPANESE", holder.voiceLanguage)
        assertEquals(1, repository.detailCalls)

        holder.selectVoiceLanguage("ENGLISH")

        assertEquals("ENGLISH", holder.voiceLanguage)
        assertEquals(2, repository.detailCalls)
    }

    @Test
    fun selectVoiceLanguageIgnoresTheSameValue() {
        val repository = RecordingCharacterRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.selectVoiceLanguage("JAPANESE")

        assertEquals(1, repository.detailCalls)
    }
}

private class GatedCharacterRepository :
    CharacterDetailRepository by RecordingCharacterRepository() {
    val gate = CompletableDeferred<Unit>()

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        gate.await()
        return NetworkResult.Success(Unit)
    }
}

/** See `MediaDetailFavouriteCacheTest`: the late response must not mix entries. */
class CharacterDetailFavouriteCacheTest {

    @Test
    fun togglingOnAnotherCharacterKeepsEachCacheEntrySeparate() {
        val repository = GatedCharacterRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)
        holder.toggleFavourite()
        assertTrue(holder.isFavourite, "optimistic")

        holder.load(2)
        assertFalse(holder.isFavourite, "the visible character is the second one")

        repository.gate.complete(Unit)

        assertFalse(holder.isFavourite, "the late response does not touch the visible one")
        holder.load(1)
        assertTrue(holder.isFavourite, "the first character's snapshot kept the toggle")
    }
}
