package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.core.domain.model.MediaSeasonYear
import com.luum.michi.app.core.domain.model.currentSeasonAndYear
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun mediaJson(
    id: Int,
    season: String? = null,
    seasonYear: Int? = null,
    listStatus: String? = null,
): JsonObject = buildJsonObject {
    put("id", id)
    if (season != null) put("season", season)
    if (seasonYear != null) put("seasonYear", seasonYear)
    if (listStatus != null) {
        put("mediaListEntry", buildJsonObject {
            put("id", id)
            put("status", listStatus)
        })
    }
}

private fun schedJson(id: Int, airingAt: Long, media: JsonObject? = null): JsonObject =
    buildJsonObject {
        put("id", id)
        put("airingAt", airingAt)
        put("episode", 1)
        if (media != null) put("media", media)
    }

private fun pageJson(hasNextPage: Boolean, vararg schedules: JsonObject): JsonElement =
    buildJsonObject {
        put("Page", buildJsonObject {
            put("pageInfo", buildJsonObject {
                put("hasNextPage", hasNextPage)
                put("currentPage", 1)
            })
            put("airingSchedules", JsonArray(schedules.toList()))
        })
    }

private fun epochUtc(year: Int, month: Int, day: Int, hour: Int): Long =
    LocalDateTime(year, month, day, hour, 0).toInstant(TimeZone.UTC).epochSeconds

private val Now = epochUtc(2025, 9, 6, 12)

private fun wiredHolder(graphQL: FakeGraphQL): Pair<CalendarStateHolder, CalendarRepository> {
    val repository = CalendarRepositoryImpl(graphQL) { Now }
    val holder = CalendarStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
    return holder to repository
}

class CalendarIntegrationTest {

    @Test
    fun happySliceLoadsOneDayWithOneItem() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(1, holder.days.size)
        assertEquals(1, holder.days.single().items.size)
        assertNull(holder.error)
        assertEquals(holder.days.single().dayBucket, holder.selectedDayBucket)
    }

    @Test
    fun currentSeasonFilterMatchesEndToEnd() {
        val current: MediaSeasonYear = currentSeasonAndYear()
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(
                    false,
                    schedJson(
                        1,
                        Now,
                        mediaJson(10, season = current.season.name, seasonYear = current.year),
                    ),
                ),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        holder.updateSeasonFilter(CalendarSeasonFilter.CURRENT)
        assertEquals(1, holder.visibleDays.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.PREVIOUS)
        assertEquals(0, holder.visibleDays.size)
    }

    @Test
    fun statusAndOtherFiltersMatchEndToEnd() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(
                    false,
                    schedJson(1, Now, mediaJson(10)),
                ),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        holder.updateSeasonFilter(CalendarSeasonFilter.OTHER)
        assertEquals(1, holder.visibleDays.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.ALL)
        holder.updateStatusFilter(CalendarStatusFilter.NOT_IN_LIST)
        assertEquals(1, holder.visibleDays.size)
        holder.updateStatusFilter(CalendarStatusFilter.WATCHING_PLANNING)
        assertEquals(0, holder.visibleDays.size)
    }

    @Test
    fun doubleLoadFetchesOnceEndToEnd() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        ).apply { gate = CompletableDeferred() }
        val (holder) = wiredHolder(graphQL)
        val gate = graphQL.gate!!
        holder.load()
        holder.load()
        graphQL.gate = null
        gate.complete(Unit)
        assertEquals(1, graphQL.calls)
        assertEquals(1, holder.days.single().items.size)
    }

    @Test
    fun serverFailureSurfacesOnHolder() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
            failureAt = 0,
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertTrue(holder.days.isEmpty())
        assertTrue(holder.error is NetworkError.Http)
    }

    @Test
    fun dayNavigationMovesOverLoadedDays() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(
                    false,
                    schedJson(1, Now, mediaJson(10)),
                    schedJson(2, Now + 72 * 3600, mediaJson(20)),
                ),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(2, holder.days.size)
        val first = holder.selectedDayBucket
        holder.stepDay(1)
        assertTrue(holder.selectedDayBucket != first)
        assertEquals(2, holder.visibleDays.size)
    }
}
