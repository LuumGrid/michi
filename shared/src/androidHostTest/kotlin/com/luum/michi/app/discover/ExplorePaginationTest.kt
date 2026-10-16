package com.luum.michi.app.discover

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun exploreResult(id: Int) = ExploreResult(
    id = id,
    title = "Item $id",
    meta = "meta",
    cover = null,
    paletteHex = null,
    averageScore = null,
)

private data class MediaCall(
    val type: MediaType,
    val sort: String,
    val page: Int,
    val query: String?,
    val genres: List<String>,
)

/**
 * Scripted [ExploreRepository]: a canned page 1 / page 2 pair, an optional gate
 * on a page number to hold a request in flight, and call recording (sort, type,
 * strings) for the wiring assertions.
 */
private class PaginationExploreRepository(
    var pageOne: List<Int> = emptyList(),
    var pageTwo: List<Int> = emptyList(),
    var pageOneHasNext: Boolean = false,
    private val gatePage: Int? = null,
) : ExploreRepository {
    val gate = CompletableDeferred<Unit>()
    val mediaCalls = mutableListOf<MediaCall>()
    var lastCharacterStrings: LanguageStrings? = null
        private set

    override suspend fun searchMedia(
        type: MediaType,
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> {
        mediaCalls += MediaCall(type, sort, page, query, genres)
        if (page == gatePage) gate.await()
        val ids = if (page == 1) pageOne else pageTwo
        val hasNext = page == 1 && pageOneHasNext
        return NetworkResult.Success(Page(ids.map { exploreResult(it) }, hasNext, page))
    }

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> = NetworkResult.Success(Page(emptyList(), false, page))

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> {
        lastCharacterStrings = strings
        if (page == gatePage) gate.await()
        return NetworkResult.Success(Page(emptyList(), false, page))
    }

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, page))

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, page))
}

private fun ids(holder: ExploreStateHolder) = holder.results.map { it.id }

private fun holderWith(
    repository: ExploreRepository,
    scope: CoroutineScope,
    strings: () -> LanguageStrings = { EnglishStrings },
) = ExploreStateHolder(repository, scope, strings)

class ExplorePaginationTest {

    @Test
    fun aPageInFlightIsDroppedByANewSearch() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val repository = PaginationExploreRepository(
                pageOne = listOf(1),
                pageTwo = listOf(99),
                pageOneHasNext = true,
                gatePage = 2,
            )
            val holder = holderWith(repository, scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), ids(holder))

            holder.loadMore() // page 2, held at the gate
            dispatcher.runAll()

            repository.pageOne = listOf(2)
            holder.load() // a new search supersedes the in-flight page
            dispatcher.runAll()

            repository.gate.complete(Unit) // the stale page 2 lands late
            dispatcher.runAll()

            assertEquals(listOf(2), ids(holder), "the old page 2 must not be merged")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aCancelledLoadDoesNotStopTheCurrentSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val repository = PaginationExploreRepository(pageOne = listOf(2), gatePage = 1)
            val holder = holderWith(repository, scope)

            holder.load() // L1, held at the gate
            dispatcher.runAll()
            assertTrue(holder.isLoading)

            holder.load() // cancels L1, starts L2
            dispatcher.runAll() // L1's cancellation runs its finally
            assertTrue(holder.isLoading, "the stale finally must not stop the current spinner")

            repository.gate.complete(Unit)
            dispatcher.runAll()

            assertFalse(holder.isLoading)
            assertEquals(listOf(2), ids(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun switchingToMangaSendsTheMangaSort() {
        val repository = PaginationExploreRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(repository, scope)

            holder.updateSort(UserListSort.PROGRESS, UserListOrder.DESCENDING, persist = false)
            holder.updateFilters(newCategory = ExploreCategory.MANGA)

            // Synchronous: a category change recomputes the derived key.
            assertEquals("CHAPTERS_DESC", holder.sort)

            // The eventual (debounced) request carries it.
            val deadline = System.currentTimeMillis() + 3000
            while (repository.mediaCalls.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }
            assertEquals(1, repository.mediaCalls.size)
            val call = repository.mediaCalls.single()
            assertEquals(MediaType.MANGA, call.type)
            assertEquals("CHAPTERS_DESC", call.sort)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aLanguageChangeLocalizesTheNextSearch() {
        var language: LanguageStrings = EnglishStrings
        val repository = PaginationExploreRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(repository, scope, strings = { language })
            holder.category = ExploreCategory.CHARACTERS

            holder.load()
            assertEquals("Character", repository.lastCharacterStrings?.characterLabel)

            language = SpanishStrings
            holder.load()
            assertEquals("Personaje", repository.lastCharacterStrings?.characterLabel)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun enterBlankSearchDropsAnInFlightPage() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val repository = PaginationExploreRepository(
                pageOne = listOf(1),
                pageTwo = listOf(99),
                pageOneHasNext = true,
                gatePage = 2,
            )
            val holder = holderWith(repository, scope)

            holder.load()
            dispatcher.runAll()
            holder.loadMore() // page 2, held at the gate
            dispatcher.runAll()
            assertTrue(holder.isLoadingMore)

            holder.enterBlankSearch()
            assertFalse(holder.isLoadingMore, "the blank search must drop the spinner")

            repository.gate.complete(Unit)
            dispatcher.runAll()

            assertTrue(holder.results.isEmpty(), "nothing from the old page lands")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun pagesThatOverlapAreDeduplicatedById() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val repository = PaginationExploreRepository(
                pageOne = listOf(1, 2),
                pageTwo = listOf(2, 3),
                pageOneHasNext = true,
            )
            val holder = holderWith(repository, scope)

            holder.load()
            dispatcher.runAll()
            holder.loadMore()
            dispatcher.runAll()

            assertEquals(listOf(1, 2, 3), ids(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aFilterChangeKeepsTheVisibleItemsAndBlocksLoadMoreDuringTheDebounce() {
        val repository = PaginationExploreRepository(pageOne = listOf(1), pageOneHasNext = true)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(repository, scope)
            holder.load()
            assertEquals(listOf(1), ids(holder))

            val callsBefore = repository.mediaCalls.size
            holder.updateFilters(newGenres = listOf("Action"))

            assertTrue(holder.isLoading)
            assertEquals(listOf(1), ids(holder), "the loaded items stay visible while typing")
            holder.loadMore()
            assertEquals(callsBefore, repository.mediaCalls.size, "no page N+1 against the old items")
        } finally {
            scope.cancel()
        }
    }
}
