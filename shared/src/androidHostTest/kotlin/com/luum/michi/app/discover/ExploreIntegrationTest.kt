package com.luum.michi.app.discover

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

private fun exploreWired(
    graphQL: FakeGraphQL,
    hideAdult: Boolean,
    category: ExploreCategory = ExploreCategory.ANIME,
): ExploreStateHolder = ExploreStateHolder(
    repository = ExploreRepositoryImpl(graphQL),
    scope = CoroutineScope(Dispatchers.Unconfined),
    strings = EnglishStrings,
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
}
