package com.luum.michi.app.discover

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.FakeGraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val emptyExplorePage = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject { put("hasNextPage", false) })
        putJsonArray("media") {}
    })
}

private val studioSearchPage = buildJsonObject {
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", false)
            put("currentPage", 1)
        })
        putJsonArray("studios") {
            add(buildJsonObject {
                put("id", 1)
                put("name", "Studio A")
            })
        }
    })
}

/** Collapses whitespace so a query-shape assertion is indentation-independent. */
private fun normalized(query: String) = query.replace(Regex("\\s+"), " ").trim()

private fun exploreWired(
    graphQL: FakeGraphQL,
    hideAdult: Boolean,
    category: ExploreCategory = ExploreCategory.ANIME,
): ExploreStateHolder = ExploreStateHolder(
    repository = ExploreRepositoryImpl(graphQL),
    scope = CoroutineScope(Dispatchers.Unconfined),
    stringsProvider = { EnglishStrings },
).apply {
    this.category = category
    this.hideAdult = hideAdult
}

class ExploreIntegrationTest {

    @Test
    fun hideAdultSendsIsAdultFalse() {
        val graphQL = FakeGraphQL(listOf(emptyExplorePage))
        val editor = exploreWired(graphQL, hideAdult = true)

        editor.load()

        assertEquals(1, graphQL.requests.size, "search fired")
        val variables = graphQL.requests.single().variables
        assertEquals("false", variables?.get("isAdult")?.toString())
    }

    @Test
    fun adultAllowedOmitsIsAdultVariable() {
        val graphQL = FakeGraphQL(listOf(emptyExplorePage))
        val editor = exploreWired(graphQL, hideAdult = false)

        editor.load()

        assertEquals(1, graphQL.requests.size, "search fired")
        val variables = graphQL.requests.single().variables
        assertFalse(variables?.containsKey("isAdult") == true, "isAdult must be omitted (null = both)")
    }

    @Test
    fun hideAdultAppliesToMangaCatalog() {
        val graphQL = FakeGraphQL(listOf(emptyExplorePage))
        val editor = exploreWired(graphQL, hideAdult = true, category = ExploreCategory.MANGA)

        editor.load()

        assertEquals(1, graphQL.requests.size, "search fired")
        val variables = graphQL.requests.single().variables
        assertEquals("false", variables?.get("isAdult")?.toString())
        assertTrue(editor.results.isEmpty())
    }

    /**
     * Tripwire: the studios search selection is exactly `{ id name }` — the old
     * `media { nodes { coverImage } }` trick must never come back. Asserted on the
     * exact normalized block, not a loose `!contains("media")` over the query.
     */
    @Test
    fun studioSearchNeverAsksForMedia() {
        val graphQL = FakeGraphQL(listOf(studioSearchPage))

        val result = runBlocking {
            ExploreRepositoryImpl(graphQL).searchStudios(query = "x", page = 1, strings = EnglishStrings)
        }

        assertTrue(
            normalized(graphQL.requests.single().query)
                .contains("studios(search: \$search, sort: \$sort) { id name }"),
            "the studio search selection must be only id + name",
        )
        assertTrue(result is NetworkResult.Success)
        assertTrue(result.value.items.single().cover == null, "a studio has no image")
    }
}
