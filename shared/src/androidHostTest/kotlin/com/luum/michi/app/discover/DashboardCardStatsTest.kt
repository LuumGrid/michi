package com.luum.michi.app.discover

import com.luum.michi.app.discover.domain.model.DashboardCardStat
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.cardStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardCardStatsTest {

    @Test
    fun popularRailsLeadWithPopularity() {
        val popular = DashboardSectionKind.entries.filter { it.name.startsWith("POPULAR_") }
        assertTrue(popular.isNotEmpty())
        popular.forEach { kind ->
            assertEquals(
                listOf(DashboardCardStat.POPULARITY, DashboardCardStat.SCORE),
                kind.cardStats(),
                kind.name,
            )
        }
    }

    @Test
    fun bestRatedAndTheRestLeadWithTheScore() {
        val others = DashboardSectionKind.entries.filterNot { it.name.startsWith("POPULAR_") }
        assertTrue(others.isNotEmpty())
        others.forEach { kind ->
            assertEquals(
                listOf(DashboardCardStat.SCORE, DashboardCardStat.POPULARITY),
                kind.cardStats(),
                kind.name,
            )
        }
    }

    @Test
    fun everyRailShowsAtMostTwoBadges() {
        DashboardSectionKind.entries.forEach { kind ->
            assertTrue(kind.cardStats().size in 1..2, kind.name)
            assertEquals(kind.cardStats().distinct(), kind.cardStats(), kind.name)
        }
    }
}
