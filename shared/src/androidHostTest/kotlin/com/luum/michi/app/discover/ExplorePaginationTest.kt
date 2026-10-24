package com.luum.michi.app.discover

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.FakeGraphQL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun mediaNode(id: Int) = buildJsonObject {
    put("id", id)
    put("title", buildJsonObject { put("userPreferred", "Item $id") })
    put("format", "TV")
}

private fun mediaPage(ids: List<Int>, hasNextPage: Boolean, currentPage: Int = 1): JsonElement =
    buildJsonObject {
        put("Page", buildJsonObject {
            put("pageInfo", buildJsonObject {
                put("hasNextPage", hasNextPage)
                put("currentPage", currentPage)
            })
            putJsonArray("media") { ids.forEach { add(mediaNode(it)) } }
        })
    }

private fun characterPage(id: Int): JsonElement = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", false)
            put("currentPage", 1)
        })
        putJsonArray("characters") {
            add(buildJsonObject {
                put("id", id)
                put("name", buildJsonObject { put("userPreferred", "C$id") })
            })
        }
    })
}

private fun ids(holder: ExploreStateHolder) = holder.results.map { it.id }

private fun holderWith(
    client: FakeGraphQL,
    scope: CoroutineScope,
    strings: () -> LanguageStrings = { EnglishStrings },
) = ExploreStateHolder(ExploreRepositoryImpl(client), scope, strings)

/**
 * Explore paging at the repository seam: a scripted [FakeGraphQL] (canned JSON)
 * behind the real repository + holder. Race tests drive [DeferredDispatcher] and
 * set [FakeGraphQL.gate] to hold requests in flight.
 */
class ExplorePaginationTest {

    @Test
    fun aPageInFlightIsDroppedByANewSearch() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(
                listOf(
                    mediaPage(listOf(1), hasNextPage = true), // page 1
                    mediaPage(listOf(99), hasNextPage = false), // stale page 2
                    mediaPage(listOf(2), hasNextPage = false), // new page 1
                ),
            )
            val holder = holderWith(client, scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), ids(holder))

            client.gate = CompletableDeferred()
            holder.loadMore() // page 2, held at the gate
            dispatcher.runAll()

            holder.load() // a new search supersedes the in-flight page
            dispatcher.runAll()

            client.gate!!.complete(Unit) // the stale page 2 lands late
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
            val client = FakeGraphQL(
                listOf(mediaPage(listOf(2), hasNextPage = false), mediaPage(listOf(2), hasNextPage = false)),
            )
            val holder = holderWith(client, scope)

            client.gate = CompletableDeferred()
            holder.load() // L1, held at the gate
            dispatcher.runAll()
            assertTrue(holder.isLoading)

            holder.load() // cancels L1, starts L2
            dispatcher.runAll() // L1's cancellation runs its finally
            assertTrue(holder.isLoading, "the stale finally must not stop the current spinner")

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertFalse(holder.isLoading)
            assertEquals(listOf(2), ids(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun switchingToMangaSendsTheMangaSort() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val client = FakeGraphQL(
                listOf(mediaPage(emptyList(), hasNextPage = false), mediaPage(emptyList(), hasNextPage = false), mediaPage(emptyList(), hasNextPage = false)),
            )
            val holder = holderWith(client, scope)

            holder.submitSearch("naruto")
            holder.changeCategory(ExploreCategory.MANGA)
            holder.updateSort(UserListSort.PROGRESS, UserListOrder.DESCENDING)

            // Synchronous: the derived key follows the manga catalog.
            assertEquals("CHAPTERS_DESC", holder.sort)

            assertEquals(3, client.requests.size)
            val variables = client.requests[2].variables
            assertEquals("MANGA", (variables?.get("type") as? JsonPrimitive)?.content)
            assertEquals("[\"CHAPTERS_DESC\"]", variables?.get("sort").toString())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aLanguageChangeLocalizesTheNextSearch() {
        var language: LanguageStrings = EnglishStrings
        val client = FakeGraphQL(listOf(characterPage(1), characterPage(1)))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope, strings = { language })
            holder.category = ExploreCategory.CHARACTERS

            holder.load()
            assertEquals("Character", holder.results.single().meta)

            language = SpanishStrings
            holder.load()
            assertEquals("Personaje", holder.results.single().meta)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun enterBlankSearchDropsAnInFlightPage() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(
                listOf(mediaPage(listOf(1), hasNextPage = true), mediaPage(listOf(99), hasNextPage = false)),
            )
            val holder = holderWith(client, scope)

            holder.load()
            dispatcher.runAll()

            client.gate = CompletableDeferred()
            holder.loadMore() // page 2, held at the gate
            dispatcher.runAll()
            assertTrue(holder.isLoadingMore)

            holder.enterBlankSearch()
            assertFalse(holder.isLoadingMore, "the blank search must drop the spinner")

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertTrue(holder.results.isEmpty(), "nothing from the old page lands")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun pagesThatOverlapAreDeduplicatedById() {
        val client = FakeGraphQL(
            listOf(
                mediaPage(listOf(1, 2), hasNextPage = true),
                mediaPage(listOf(2, 3), hasNextPage = false, currentPage = 2),
            ),
        )
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            holder.load()
            holder.loadMore()

            assertEquals(listOf(1, 2, 3), ids(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun applyingFiltersClearsTheOldResultsAndReloads() {
        val client = FakeGraphQL(
            listOf(mediaPage(listOf(1), hasNextPage = true), mediaPage(listOf(2), hasNextPage = false)),
        )
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)
            holder.submitSearch("naruto")
            assertEquals(listOf(1), ids(holder))

            holder.applyFilters(ExploreFilters(genres = listOf("Action")))

            assertEquals(listOf(2), ids(holder), "the old page is dropped and replaced")
        } finally {
            scope.cancel()
        }
    }
}
