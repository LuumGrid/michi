package com.luum.michi.app.discover

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
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

private fun mediaPage(ids: List<Int>, hasNextPage: Boolean): JsonElement = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", hasNextPage)
            put("currentPage", 1)
        })
        putJsonArray("media") {
            ids.forEach { id ->
                add(buildJsonObject {
                    put("id", id)
                    put("title", buildJsonObject { put("userPreferred", "Item $id") })
                    put("format", "TV")
                })
            }
        }
    })
}

private fun holderWith(client: FakeGraphQL, scope: CoroutineScope) = ExploreStateHolder(
    repository = ExploreRepositoryImpl(client),
    scope = scope,
    stringsProvider = { EnglishStrings },
)

/**
 * The submit-driven search at the repository seam: typing never reaches the
 * holder (the field lives in Root's State), only a submit or a chip change
 * spends a request, and a blank/unchanged submit is a no-op.
 */
class ExploreSearchTest {

    @Test
    fun typingWithoutSubmittingFiresNothing() {
        val client = FakeGraphQL(listOf(mediaPage(listOf(1), hasNextPage = false)))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            // Explore isn't visible and no request was made before a submit.
            assertFalse(holder.hasSearched)
            assertEquals(0, client.calls)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aBlankSubmitIsANoOp() {
        val client = FakeGraphQL(listOf(mediaPage(listOf(1), hasNextPage = false)))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            holder.submitSearch("   ")

            assertEquals(0, client.calls)
            assertFalse(holder.hasSearched)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun submittingFiresOneRequestAndShowsResults() {
        val client = FakeGraphQL(listOf(mediaPage(listOf(1), hasNextPage = false)))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            holder.submitSearch("  naruto  ")

            assertEquals(1, client.calls)
            assertTrue(holder.hasSearched)
            assertEquals("naruto", holder.query, "the query is trimmed")
            assertEquals(listOf(1), holder.results.map { it.id })
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun resubmittingTheSameQueryAndCategoryIsDeduped() {
        val client = FakeGraphQL(listOf(mediaPage(listOf(1), hasNextPage = false)))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            holder.submitSearch("naruto")
            holder.submitSearch("naruto")

            assertEquals(1, client.calls)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun changingChipFiresOneRequestForTheNewCategory() {
        val client = FakeGraphQL(
            listOf(
                mediaPage(listOf(1), hasNextPage = false), // ANIME
                mediaPage(listOf(2), hasNextPage = false), // MANGA
            ),
        )
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val holder = holderWith(client, scope)

            holder.submitSearch("naruto")
            holder.changeCategory(ExploreCategory.MANGA)

            assertEquals(2, client.calls)
            assertEquals(listOf(2), holder.results.map { it.id }, "the old results are dropped")
            val type = client.requests[1].variables?.get("type") as? JsonPrimitive
            assertEquals("MANGA", type?.content)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun clearingTheFieldDuringAnInFlightLoadCancelsItAndStopsTheSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(mediaPage(listOf(1), hasNextPage = false)))
            client.gate = CompletableDeferred()
            val holder = holderWith(client, scope)

            holder.submitSearch("naruto")
            dispatcher.runAll()
            assertTrue(holder.isLoading)

            holder.enterBlankSearch()
            assertFalse(holder.isLoading, "the blank search drops the spinner itself")
            assertFalse(holder.hasSearched)

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertTrue(holder.results.isEmpty(), "nothing from the cancelled load lands")
        } finally {
            scope.cancel()
        }
    }
}
