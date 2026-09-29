package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

private fun loadDay(
    repo: CalendarRepositoryImpl,
    from: Long = 1_700_000_000L,
    to: Long = from + 86399,
): NetworkResult<List<CalendarEntry>> = runBlocking { repo.loadDay(from, to) }

class CalendarRepositoryTest {

    @Test
    fun pagesCombineIntoOneDaySlice() {
        val base = 1_700_000_000L
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(
                listOf(
                    pageData(true, sched(1, base)),
                    pageData(false, sched(2, base + 3600)),
                ),
            ),
        )
        val result = loadDay(repo, base, base + 86399)

        assertIs<NetworkResult.Success<List<CalendarEntry>>>(result)
        assertEquals(listOf(1, 2), result.value.map { it.scheduleId })
    }

    @Test
    fun failureMidLoopReturnsFailure() {
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(
                listOf(pageData(true, sched(1, 1_700_000_000L))),
                failureAt = 1,
            ),
        )
        val result = loadDay(repo)

        assertTrue(result is NetworkResult.Failure)
    }

    @Test
    fun nullPageYieldsEmptySlice() {
        val repo = CalendarRepositoryImpl(
            FakeGraphQL(listOf(buildJsonObject { put("Page", JsonNull) })),
        )
        val result = loadDay(repo)

        assertIs<NetworkResult.Success<List<CalendarEntry>>>(result)
        assertTrue(result.value.isEmpty())
    }

    @Test
    fun queryVariablesCarryDayWindowAndPaging() {
        val from = 1_700_000_000L
        val to = from + 86399
        val fake = FakeGraphQL(
            listOf(
                pageData(true, sched(1, from)),
                pageData(false, sched(2, from + 3600)),
            ),
        )
        loadDay(CalendarRepositoryImpl(fake), from, to)

        assertEquals(2, fake.requests.size)
        assertEquals(from, fake.requests[0].variables!!["from"]?.jsonPrimitive?.long)
        assertEquals(to, fake.requests[0].variables!!["to"]?.jsonPrimitive?.long)
        assertEquals(1, fake.requests[0].variables!!["page"]?.jsonPrimitive?.int)
        assertEquals(2, fake.requests[1].variables!!["page"]?.jsonPrimitive?.int)
    }
}
