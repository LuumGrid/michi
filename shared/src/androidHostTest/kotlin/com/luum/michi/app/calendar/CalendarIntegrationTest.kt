package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.localMidnightEpoch
import com.luum.michi.app.core.model.next
import com.luum.michi.app.core.model.previous
import com.luum.michi.app.core.network.domain.NetworkError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
    val repository = CalendarRepositoryImpl(graphQL)
    val holder = CalendarStateHolder(
        repository,
        CoroutineScope(Dispatchers.Unconfined),
        nowProvider = { Now },
    )
    return holder to repository
}

class CalendarIntegrationTest {

    @Test
    fun happySliceLoadsTodayWithOneItem() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        assertEquals(1, graphQL.calls)
        assertEquals(1, holder.selectedItems.size)
        assertNull(holder.error)
        assertEquals(holder.todayBucket, holder.selectedDayBucket)
    }

    @Test
    fun cachedDayReselectSkipsRequest() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(false, schedJson(1, Now, mediaJson(10))),
                pageJson(false),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(1, graphQL.calls)

        // Same day again: cache hit, no request.
        holder.selectDay(holder.todayBucket)
        assertEquals(1, graphQL.calls)

        // Next day: miss, one request (empty slice).
        holder.stepDay(1)
        assertEquals(2, graphQL.calls)
        assertEquals(0, holder.selectedItems.size)

        // Back to today: cache hit, items restored with no request.
        holder.stepDay(-1)
        assertEquals(2, graphQL.calls)
        assertEquals(1, holder.selectedItems.size)
    }

    @Test
    fun reloadSelectedBypassesCache() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(false, schedJson(1, Now, mediaJson(10))),
                pageJson(false, schedJson(1, Now, mediaJson(10))),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(1, graphQL.calls)

        holder.reloadSelected()
        assertEquals(2, graphQL.calls)
        assertEquals(1, holder.selectedItems.size)
    }

    @Test
    fun dayQueryCarriesMidnightSliceWindow() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        val variables = graphQL.requests.single().variables!!
        val from = variables["from"].toString().toLong()
        val to = variables["to"].toString().toLong()
        assertEquals(holder.todayBucket, from)
        assertTrue(to > from)
        assertTrue(to - from <= 86400)
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
        assertEquals(1, holder.selectedItems.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.PREVIOUS)
        assertEquals(0, holder.selectedItems.size)
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
        assertEquals(1, holder.selectedItems.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.ALL)
        holder.updateStatusFilter(CalendarStatusFilter.NOT_IN_LIST)
        assertEquals(1, holder.selectedItems.size)
        holder.updateStatusFilter(CalendarStatusFilter.WATCHING_PLANNING)
        assertEquals(0, holder.selectedItems.size)
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
        assertEquals(1, holder.selectedItems.size)
    }

    @Test
    fun serverFailureSurfacesOnHolder() {
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
            failureAt = 0,
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(0, holder.selectedItems.size)
        assertTrue(holder.error is NetworkError.Http)
    }

    @Test
    fun dayNavigationStepsCalendarDaysIncludingEmpty() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(false, schedJson(1, Now, mediaJson(10))),
                pageJson(false),
                pageJson(false, schedJson(2, Now + 72 * 3600, mediaJson(20))),
                pageJson(false),
                pageJson(false),
                pageJson(false),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        val zone = TimeZone.currentSystemDefault()
        assertEquals(localMidnightEpoch(Now, zone) - 86400, holder.minDayBucket)
        assertTrue(holder.maxDayBucket > holder.selectedDayBucket!!)

        // Arrows traverse calendar days, not releases: the middle day is
        // empty but selectable (Otraku parity).
        holder.stepDay(1)
        assertEquals(localMidnightEpoch(Now + 86400, zone), holder.selectedDayBucket)
        assertEquals(0, holder.selectedItems.size)
        holder.stepDay(1)
        assertEquals(localMidnightEpoch(Now + 2 * 86400, zone), holder.selectedDayBucket)
        assertEquals(20, holder.selectedItems.single().item.id)
        holder.stepDay(1)
        assertEquals(0, holder.selectedItems.size)

        // Clamp at both ends of [minDayBucket, maxDayBucket].
        holder.stepDay(999)
        assertEquals(holder.maxDayBucket, holder.selectedDayBucket)
        holder.stepDay(-999)
        assertEquals(holder.minDayBucket, holder.selectedDayBucket)
        assertEquals(6, graphQL.calls)
    }

    @Test
    fun previousAndNextSeasonFiltersMatchEndToEnd() {
        val current = currentSeasonAndYear()
        val previous = current.previous()
        val next = current.next()
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(
                    false,
                    schedJson(
                        1,
                        Now,
                        mediaJson(10, season = previous.season.name, seasonYear = previous.year),
                    ),
                    schedJson(
                        2,
                        Now,
                        mediaJson(20, season = next.season.name, seasonYear = next.year),
                    ),
                ),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        assertEquals(2, holder.selectedItems.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.PREVIOUS)
        assertEquals(1, holder.selectedItems.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.NEXT)
        assertEquals(1, holder.selectedItems.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.CURRENT)
        assertEquals(0, holder.selectedItems.size)
    }

    @Test
    fun watchingPlanningFilterMatchesEndToEnd() {
        val graphQL = FakeGraphQL(
            listOf(
                pageJson(
                    false,
                    schedJson(1, Now, mediaJson(10, listStatus = "CURRENT")),
                    schedJson(2, Now, mediaJson(20, listStatus = "PAUSED")),
                ),
            ),
        )
        val (holder) = wiredHolder(graphQL)
        holder.load()
        holder.updateStatusFilter(CalendarStatusFilter.WATCHING_PLANNING)
        assertEquals(1, holder.selectedItems.size)
        assertEquals(
            10,
            holder.selectedItems.single().item.id,
        )
    }

    @Test
    fun guestEntriesWithoutListEntryMapToNullUserFields() {
        // Live guest shape: airing data is public but mediaListEntry (and
        // isFavourite) resolve null without a session. The feed must still
        // decode and render with null user fields.
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        val item = holder.selectedItems.single().item
        assertNull(item.userStatus)
        assertEquals(false, item.isUserFavorited)
        assertEquals(false, item.isUserRanked)
        assertEquals(1, holder.selectedItems.size)
    }

    @Test
    fun authenticatedEntryShapeMapsUserFields() {
        // Live logged-in shape: mediaListEntry resolves with id + status +
        // score. The entry id selection is required by the shared DTO, so a
        // populated entry must decode and map (this bit the calendar once:
        // the query omitted id and every logged-in load failed).
        val entryJson = buildJsonObject {
            put("id", 7)
            put("status", "CURRENT")
            put("score", 8.5)
        }
        val media = buildJsonObject {
            put("id", 10)
            put("mediaListEntry", entryJson)
        }
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, media))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        val item = holder.selectedItems.single().item
        assertEquals(MediaListStatus.CURRENT, item.userStatus)
        assertEquals(true, item.isUserRanked)
    }

    @Test
    fun entryIdSelectionIsInTheQuery() {
        // The shared entry DTO requires id: guard the selection text so a
        // future edit cannot drop it again (fakes don't validate selections).
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, mediaJson(10)))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        val query = graphQL.requests.single().query
        assertTrue(query.contains("mediaListEntry { id status progress score }"))
        assertTrue(query.contains("status"))
    }

    @Test
    fun releaseMetadataMapsFormatStatusScoreAndPlatforms() {
        // Card metadata mirrors the lists: format + work status drive the
        // meta line, the entry score drives the user pill, and only
        // enabled STREAMING links become platform chips.
        val media = buildJsonObject {
            put("id", 10)
            put("format", "TV")
            put("status", "RELEASING")
            put("season", "SUMMER")
            put("seasonYear", 2026)
            put(
                "mediaListEntry",
                buildJsonObject {
                    put("id", 7)
                    put("status", "CURRENT")
                    put("score", 8.5)
                },
            )
            put(
                "externalLinks",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("site", "Crunchyroll")
                            put("url", "https://example.com/cr")
                            put("type", "STREAMING")
                            put("color", "#F47521")
                        },
                        buildJsonObject {
                            put("site", "Dead")
                            put("url", "https://example.com/dead")
                            put("type", "STREAMING")
                            put("isDisabled", true)
                        },
                        buildJsonObject {
                            put("site", "Info")
                            put("url", "https://example.com/info")
                            put("type", "INFO")
                        },
                    ),
                ),
            )
        }
        val graphQL = FakeGraphQL(
            listOf(pageJson(false, schedJson(1, Now, media))),
        )
        val (holder) = wiredHolder(graphQL)

        holder.load()

        val item = holder.selectedItems.single().item
        assertEquals(MediaFormat.TV, item.format)
        assertEquals(MediaWorkStatus.RELEASING, item.mediaStatus)
        assertEquals(8.5f, item.userScore)
        assertEquals(listOf("Crunchyroll"), item.streamingPlatforms.map { it.site })
        assertEquals("https://example.com/cr", item.streamingPlatforms.single().url)
        assertEquals("#F47521", item.streamingPlatforms.single().color)
    }
}
