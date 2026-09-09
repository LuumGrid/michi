package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sched(id: Int, airingAt: Long): JsonObject = buildJsonObject {
    put("id", id)
    put("airingAt", airingAt)
    put("episode", 1)
    put("media", buildJsonObject { put("id", id) })
}

private fun pageData(hasNextPage: Boolean, vararg schedules: JsonObject): JsonElement =
    buildJsonObject {
        put("Page", buildJsonObject {
            put("pageInfo", buildJsonObject {
                put("hasNextPage", hasNextPage)
                put("currentPage", 1)
            })
            put("airingSchedules", JsonArray(schedules.toList()))
        })
    }

private fun collect(repo: CalendarRepositoryImpl): List<NetworkResult<CalendarFeed>> =
    runBlocking { repo.loadFeed().toList() }

class CalendarRepositoryTest {

    @Test
    fun emitsProgressivelyPerPage() {
        // 72h apart: different local days in every time zone.
        val base = 1_700_000_000L
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(
                listOf(
                    pageData(true, sched(1, base)),
                    pageData(false, sched(2, base + 72 * 3600)),
                ),
            ),
        )
        val emissions = collect(repo)
        assertEquals(2, emissions.size)
        val first = emissions[0] as NetworkResult.Success
        val second = emissions[1] as NetworkResult.Success
        assertEquals(1, first.value.days.size)
        assertEquals(2, second.value.days.size)
        assertEquals(2, second.value.days.sumOf { it.items.size })
    }

    @Test
    fun failureMidLoopEmitsAfterSuccess() {
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(
                listOf(pageData(true, sched(1, 1_700_000_000L))),
                failureAt = 1,
            ),
        )
        val emissions = collect(repo)
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is NetworkResult.Success)
        assertTrue(emissions[1] is NetworkResult.Failure)
    }

    @Test
    fun nullPageYieldsEmptyFeed() {
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(listOf(buildJsonObject { put("Page", JsonNull) })),
        )
        val emissions = collect(repo)
        assertEquals(1, emissions.size)
        val success = emissions[0] as NetworkResult.Success
        assertTrue(success.value.days.isEmpty())
    }
}
